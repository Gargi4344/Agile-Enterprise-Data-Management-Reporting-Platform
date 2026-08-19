package com.aedmp.platform.job;

import com.aedmp.platform.common.exception.BadRequestException;
import com.aedmp.platform.job.dto.CreateJobRequest;
import com.aedmp.platform.job.dto.JobResponse;
import com.aedmp.platform.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    @PreAuthorize("hasAuthority('PROCESS_APPROVE')")
    @ResponseStatus(HttpStatus.CREATED)
    public JobResponse submit(@Valid @RequestBody CreateJobRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
        JobType jobType = parseJobType(request.jobType());
        return JobResponse.from(jobService.submit(request.datasetVersionId(), jobType, principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DATASET_VIEW')")
    public JobResponse get(@PathVariable Long id) {
        return JobResponse.from(jobService.get(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DATASET_VIEW')")
    public List<JobResponse> list(@RequestParam(required = false) Long datasetVersionId,
                                   @RequestParam(required = false) JobStatus status) {
        return jobService.list(datasetVersionId, status).stream().map(JobResponse::from).toList();
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAuthority('PROCESS_APPROVE')")
    public JobResponse retry(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser principal) {
        return JobResponse.from(jobService.retry(id, principal));
    }

    private JobType parseJobType(String value) {
        try {
            return JobType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown jobType: " + value);
        }
    }
}
