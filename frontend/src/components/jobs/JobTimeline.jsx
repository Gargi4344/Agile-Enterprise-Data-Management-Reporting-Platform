import { StatusBadge } from '../common/StatusBadge.jsx';
import { RoleGate } from '../common/RoleGate.jsx';
import { formatDateTime } from '../../utils/format.js';
import './JobTimeline.css';

const JOB_TYPE_LABEL = {
  VALIDATION: 'Validation',
  TRANSFORMATION: 'Transformation',
  LOAD: 'Load',
};

/**
 * Vertical timeline of processing jobs (PENDING -> RUNNING -> COMPLETED|FAILED, with
 * FAILED -> RETRYING -> RUNNING per docs/state-machine.md), each entry showing every
 * timestamp the job has reached plus retry affordances.
 */
export function JobTimeline({ jobs, onRetry, retryingId }) {
  return (
    <ol className="job-timeline">
      {jobs.map((job) => (
        <li key={job.id} className={`job-timeline-item job-timeline-${job.status?.toLowerCase()}`}>
          <div className="job-timeline-dot" aria-hidden="true" />
          <div className="job-timeline-body">
            <div className="row-between">
              <div className="row">
                <strong>{JOB_TYPE_LABEL[job.jobType] || job.jobType}</strong>
                <StatusBadge status={job.status} />
                {job.retryCount > 0 && (
                  <span className="badge badge-neutral">
                    <span className="badge-dot" aria-hidden="true" />
                    retry ×{job.retryCount}
                  </span>
                )}
              </div>
              {job.status === 'FAILED' && (
                <RoleGate permission="PROCESS_APPROVE" fallbackTitle="Requires PROCESS_APPROVE permission">
                  <button
                    type="button"
                    className="btn btn-sm btn-primary"
                    onClick={() => onRetry?.(job)}
                    disabled={retryingId === job.id}
                  >
                    {retryingId === job.id ? 'Retrying…' : 'Retry'}
                  </button>
                </RoleGate>
              )}
            </div>
            <div className="job-timeline-meta">
              <span>Dataset version #{job.datasetVersionId}</span>
              <span>Job #{job.id}</span>
            </div>
            <dl className="job-timeline-timestamps">
              <div>
                <dt>Created</dt>
                <dd>{formatDateTime(job.createdAt)}</dd>
              </div>
              <div>
                <dt>Started</dt>
                <dd>{formatDateTime(job.startedAt)}</dd>
              </div>
              <div>
                <dt>Finished</dt>
                <dd>{formatDateTime(job.completedAt || job.finishedAt)}</dd>
              </div>
            </dl>
            {job.errorMessage && <p className="job-timeline-error">{job.errorMessage}</p>}
          </div>
        </li>
      ))}
    </ol>
  );
}
