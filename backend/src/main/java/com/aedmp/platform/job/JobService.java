package com.aedmp.platform.job;

import com.aedmp.platform.audit.AuditResult;
import com.aedmp.platform.audit.AuditService;
import com.aedmp.platform.common.exception.BadRequestException;
import com.aedmp.platform.common.exception.ConflictException;
import com.aedmp.platform.common.exception.NotFoundException;
import com.aedmp.platform.dataset.DatasetVersion;
import com.aedmp.platform.dataset.DatasetVersionService;
import com.aedmp.platform.dataset.DatasetVersionStatus;
import com.aedmp.platform.dataset.csv.CsvDatasetReader;
import com.aedmp.platform.dataset.csv.CsvTable;
import com.aedmp.platform.dataset.csv.CsvWriterUtil;
import com.aedmp.platform.dataset.storage.FileStorageService;
import com.aedmp.platform.job.transform.TransformationExecutor;
import com.aedmp.platform.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class JobService {

    private final ProcessingJobRepository jobRepository;
    private final DatasetVersionService versionService;
    private final TransformationRuleRepository transformationRuleRepository;
    private final FileStorageService fileStorageService;
    private final CsvDatasetReader csvDatasetReader;
    private final CsvWriterUtil csvWriterUtil;
    private final TransformationExecutor transformationExecutor;
    private final AuditService auditService;

    public ProcessingJob get(Long id) {
        return jobRepository.findById(id).orElseThrow(() -> new NotFoundException("Job " + id + " not found"));
    }

    public List<ProcessingJob> list(Long datasetVersionId, JobStatus status) {
        if (datasetVersionId != null) {
            return jobRepository.findByDatasetVersionIdOrderByCreatedAtDesc(datasetVersionId);
        }
        if (status != null) {
            return jobRepository.findByStatusOrderByCreatedAtDesc(status);
        }
        return jobRepository.findAll();
    }

    @Transactional
    public ProcessingJob submit(Long datasetVersionId, JobType jobType, AuthenticatedUser principal) {
        if (jobType == JobType.VALIDATION) {
            throw new BadRequestException("Submit VALIDATION work via POST /api/validation/runs, not /api/jobs");
        }
        DatasetVersion version = versionService.get(datasetVersionId);
        String idempotencyKey = datasetVersionId + ":" + jobType;

        var existing = jobRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            ProcessingJob job = existing.get();
            if (job.getStatus() == JobStatus.FAILED) {
                throw new ConflictException("Job " + job.getId() + " already failed for this version/type; retry via POST /api/jobs/"
                        + job.getId() + "/retry");
            }
            return job; // PENDING/RUNNING/RETRYING/COMPLETED - idempotent replay, no duplicate work.
        }

        if (!version.getStatus().canTransitionTo(DatasetVersionStatus.PROCESSING)) {
            throw new ConflictException("Dataset version " + datasetVersionId + " must be READY before processing (currently "
                    + version.getStatus() + ")");
        }

        ProcessingJob job = jobRepository.save(ProcessingJob.builder()
                .datasetVersion(version)
                .jobType(jobType)
                .status(JobStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .retryCount(0)
                .maxRetries(3)
                .createdBy(principal.getUser())
                .build());

        return execute(job, version, principal);
    }

    @Transactional
    public ProcessingJob retry(Long jobId, AuthenticatedUser principal) {
        ProcessingJob job = get(jobId);
        if (job.getStatus() != JobStatus.FAILED) {
            throw new ConflictException("Only a FAILED job can be retried (current status: " + job.getStatus() + ")");
        }
        if (job.getRetryCount() >= job.getMaxRetries()) {
            throw new ConflictException("Job " + jobId + " has exhausted its retry budget (" + job.getMaxRetries()
                    + ") and requires manual review");
        }
        job.setRetryCount(job.getRetryCount() + 1);
        job.setStatus(JobStatus.RETRYING);
        job.setErrorMessage(null);
        jobRepository.save(job);

        DatasetVersion version = versionService.get(job.getDatasetVersion().getId());
        versionService.transition(version.getId(), DatasetVersionStatus.PROCESSING);
        return execute(job, version, principal);
    }

    private ProcessingJob execute(ProcessingJob job, DatasetVersion version, AuthenticatedUser principal) {
        if (job.getStatus() == JobStatus.PENDING) {
            versionService.transition(version.getId(), DatasetVersionStatus.PROCESSING);
        }
        job.setStatus(JobStatus.RUNNING);
        job.setStartedAt(Instant.now());
        jobRepository.save(job);

        try {
            CsvTable input = csvDatasetReader.read(fileStorageService.load(version.getStorageLocation()));
            List<TransformationRule> rules = transformationRuleRepository
                    .findByDatasetIdAndActiveTrueOrderByOrdinalPositionAsc(version.getDataset().getId());
            CsvTable transformed = transformationExecutor.apply(input, rules);
            byte[] output = csvWriterUtil.write(transformed);
            var stored = fileStorageService.store(String.valueOf(version.getDataset().getId()),
                    "v" + version.getVersionNumber() + "-processed-job" + job.getId(),
                    version.getSourceFilename(), new ByteArrayInputStream(output));

            job.setStatus(JobStatus.COMPLETED);
            job.setCompletedAt(Instant.now());
            job.setTransformationVersion(rules.isEmpty() ? null : rules.get(0).getVersion());
            jobRepository.save(job);
            versionService.transition(version.getId(), DatasetVersionStatus.COMPLETED);

            auditService.record(job.getJobType() + "_JOB_COMPLETE", "PROCESSING_JOB", String.valueOf(job.getId()),
                    AuditResult.SUCCESS, job.getId(), Map.of("datasetVersionId", version.getId(),
                            "outputLocation", stored.location(), "rowCount", transformed.rows().size()));
        } catch (Exception e) {
            job.setStatus(JobStatus.FAILED);
            job.setCompletedAt(Instant.now());
            job.setErrorMessage(safeMessage(e));
            jobRepository.save(job);
            versionService.transition(version.getId(), DatasetVersionStatus.FAILED);

            auditService.record(job.getJobType() + "_JOB_FAIL", "PROCESSING_JOB", String.valueOf(job.getId()),
                    AuditResult.FAILURE, job.getId(), Map.of("datasetVersionId", version.getId(),
                            "error", safeMessage(e)));
        }
        return job;
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            message = e.getClass().getSimpleName();
        }
        return message.length() > 1900 ? message.substring(0, 1900) : message;
    }
}
