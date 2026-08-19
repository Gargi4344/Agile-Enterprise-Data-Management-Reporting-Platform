package com.aedmp.platform.report;

import com.aedmp.platform.audit.AuditLogRepository;
import com.aedmp.platform.audit.AuditResult;
import com.aedmp.platform.audit.AuditService;
import com.aedmp.platform.common.exception.NotFoundException;
import com.aedmp.platform.dataset.DatasetRepository;
import com.aedmp.platform.dataset.DatasetVersion;
import com.aedmp.platform.dataset.DatasetVersionRepository;
import com.aedmp.platform.dataset.DatasetVersionStatus;
import com.aedmp.platform.job.JobStatus;
import com.aedmp.platform.job.ProcessingJob;
import com.aedmp.platform.job.ProcessingJobRepository;
import com.aedmp.platform.report.dto.KpiOverviewResponse;
import com.aedmp.platform.security.AuthenticatedUser;
import com.aedmp.platform.validation.ValidationRun;
import com.aedmp.platform.validation.ValidationRunRepository;
import com.aedmp.platform.validation.RuleSeverity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final DatasetVersionRepository versionRepository;
    private final DatasetRepository datasetRepository;
    private final ValidationRunRepository validationRunRepository;
    private final com.aedmp.platform.validation.ValidationResultRepository validationResultRepository;
    private final ProcessingJobRepository processingJobRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public List<Report> listForDataset(Long datasetId) {
        return reportRepository.findByDatasetIdOrderByGeneratedAtDesc(datasetId);
    }

    public Report get(Long id) {
        return reportRepository.findById(id).orElseThrow(() -> new NotFoundException("Report " + id + " not found"));
    }

    @Transactional
    public Report generate(Long datasetVersionId, String reportType, AuthenticatedUser principal) {
        DatasetVersion version = versionRepository.findById(datasetVersionId)
                .orElseThrow(() -> new NotFoundException("Dataset version " + datasetVersionId + " not found"));

        List<ValidationRun> runs = validationRunRepository.findByDatasetVersionIdOrderByStartedAtDesc(datasetVersionId);
        ValidationRun latestRun = runs.isEmpty() ? null : runs.get(0);

        List<ProcessingJob> jobs = processingJobRepository.findByDatasetVersionIdOrderByCreatedAtDesc(datasetVersionId);
        ProcessingJob latestJob = jobs.isEmpty() ? null : jobs.get(0);

        ObjectNode metrics = objectMapper.createObjectNode();
        metrics.put("datasetName", version.getDataset().getName());
        metrics.put("versionNumber", version.getVersionNumber());
        metrics.put("versionStatus", version.getStatus().name());
        metrics.put("sourceFilename", version.getSourceFilename());
        metrics.put("checksumSha256", version.getChecksumSha256());
        if (latestRun != null) {
            metrics.put("qualityScore", latestRun.getQualityScore());
            metrics.put("totalRecords", latestRun.getTotalRecords());
            metrics.put("acceptedRecords", latestRun.getAcceptedRecords());
            metrics.put("rejectedRecords", latestRun.getRejectedRecords());
            metrics.put("validationStatus", latestRun.getStatus().name());
        }
        if (latestJob != null) {
            metrics.put("jobStatus", latestJob.getStatus().name());
            metrics.put("jobType", latestJob.getJobType().name());
            metrics.put("retryCount", latestJob.getRetryCount());
        }
        metrics.put("disclaimer", "Synthetic demo data only - not a real customer dataset.");

        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("datasetVersionId", datasetVersionId);
        parameters.put("reportType", reportType != null ? reportType : "DATASET_QUALITY");

        Report report = Report.builder()
                .name(version.getDataset().getName() + " - v" + version.getVersionNumber() + " report")
                .dataset(version.getDataset())
                .datasetVersion(version)
                .reportType(reportType != null ? reportType : "DATASET_QUALITY")
                .status(ReportStatus.GENERATED)
                .generatedBy(principal.getUser())
                .generatedAt(Instant.now())
                .parameters(parameters)
                .summaryMetrics(metrics)
                .build();
        Report saved = reportRepository.save(report);
        auditService.record("REPORT_GENERATE", "REPORT", String.valueOf(saved.getId()), AuditResult.SUCCESS);
        return saved;
    }

    public KpiOverviewResponse kpiOverview() {
        long datasetsRegistered = datasetRepository.count();
        long activeDatasetVersions = versionRepository.countByStatus(DatasetVersionStatus.READY)
                + versionRepository.countByStatus(DatasetVersionStatus.PROCESSING)
                + versionRepository.countByStatus(DatasetVersionStatus.COMPLETED);
        long validationRuns = validationRunRepository.count();

        List<ValidationRun> scoredRuns = validationRunRepository.findByQualityScoreIsNotNull();
        BigDecimal qualityRate = scoredRuns.isEmpty() ? BigDecimal.ZERO
                : scoredRuns.stream().map(ValidationRun::getQualityScore).reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(scoredRuns.size()), 2, RoundingMode.HALF_UP);

        long failedRuleChecks = validationResultRepository.countBySeverityAndPassedFalse(RuleSeverity.BLOCKER);
        long recordsProcessed = scoredRuns.stream().mapToLong(ValidationRun::getTotalRecords).sum();

        List<ProcessingJob> completedJobs = processingJobRepository.findByStatus(JobStatus.COMPLETED);
        Double avgDurationSeconds = completedJobs.isEmpty() ? null : completedJobs.stream()
                .filter(j -> j.getStartedAt() != null && j.getCompletedAt() != null)
                .mapToLong(j -> Duration.between(j.getStartedAt(), j.getCompletedAt()).toMillis())
                .average().orElse(0) / 1000.0;

        long failedJobs = processingJobRepository.countByStatus(JobStatus.FAILED);
        long successfulJobs = processingJobRepository.countByStatus(JobStatus.COMPLETED);
        long reportsGenerated = reportRepository.count();

        List<KpiOverviewResponse.RecentActivityItem> recentActivity = auditLogRepository
                .findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt")))
                .stream()
                .map(a -> new KpiOverviewResponse.RecentActivityItem(a.getAction(), a.getTargetType(), a.getTargetId(),
                        a.getResult().name(), a.getCreatedAt()))
                .toList();

        return new KpiOverviewResponse(datasetsRegistered, activeDatasetVersions, validationRuns, qualityRate,
                failedRuleChecks, recordsProcessed, avgDurationSeconds, failedJobs, successfulJobs, reportsGenerated,
                recentActivity, Instant.now());
    }
}
