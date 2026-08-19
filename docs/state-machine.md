# Dataset Version & Job State Machines

## Dataset version lifecycle

```
UPLOADED --(schema profiling ok)--> VALIDATING --(no blocker failures)--> READY
VALIDATING --(blocker failure)--> REJECTED
REJECTED --(new validation run requested)--> VALIDATING
READY --(processing job started)--> PROCESSING
PROCESSING --(transform+load succeed)--> COMPLETED
PROCESSING --(unexpected exception)--> FAILED
FAILED --(retry job)--> PROCESSING
FAILED --(manual review, no retry)--> stays FAILED, visible in Jobs/Audit for review
COMPLETED --(report generated)--> COMPLETED (reports reference the version, no further transition)
```

Enforced in `DatasetVersionStatus` + `DatasetVersionService#transition(...)`, which rejects any
transition not present in the allow-list and wraps the check + write in one transaction so a version
can never be observed in an invalid intermediate state.

## Processing job lifecycle

```
PENDING --(worker picks up)--> RUNNING
RUNNING --(success)--> COMPLETED
RUNNING --(exception)--> FAILED
FAILED --(POST /api/jobs/{id}/retry, retry_count < max_retries)--> RETRYING --> RUNNING
FAILED --(retry_count >= max_retries)--> stays FAILED, requires manual review
```

## Idempotency

- Each processing job has a unique `idempotency_key`, derived as `{datasetVersionId}:{jobType}` for
  the initial submission. Submitting a job for a version/type that already has a non-terminal
  (`PENDING`/`RUNNING`/`RETRYING`) job returns the existing job (200) instead of creating a duplicate.
- Retrying increments `retry_count` and clears `error_message` on success; the final error is kept
  on the row when retries are exhausted.
- All job status writes happen inside a single `@Transactional` boundary alongside the audit log
  entry, so a crash between "job completed" and "audit recorded" cannot happen.

## Quality score

`quality_score = accepted_records / total_records * 100`, computed per validation run and persisted
on `validation_runs.quality_score`. A record is "accepted" if it has zero `BLOCKER`-severity rule
failures for that run (`WARNING`-severity failures do not reject a record, only lower confidence and
appear in the failure breakdown). Weighted variants, if enabled, multiply each rule's contribution to
a *warning* quality index by `validation_rules.weight`; the acceptance definition itself always uses
blocker/non-blocker outcome, never a weighted threshold, so "accepted" stays unambiguous.
