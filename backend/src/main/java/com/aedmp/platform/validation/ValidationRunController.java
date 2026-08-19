package com.aedmp.platform.validation;

import com.aedmp.platform.common.web.PageResponse;
import com.aedmp.platform.security.AuthenticatedUser;
import com.aedmp.platform.validation.dto.CreateValidationRunRequest;
import com.aedmp.platform.validation.dto.ValidationResultResponse;
import com.aedmp.platform.validation.dto.ValidationRunResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/validation/runs")
@RequiredArgsConstructor
public class ValidationRunController {

    private final ValidationRunService runService;

    @PostMapping
    @PreAuthorize("hasAuthority('VALIDATION_RUN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ValidationRunResponse create(@Valid @RequestBody CreateValidationRunRequest request,
                                         @AuthenticationPrincipal AuthenticatedUser principal) {
        return ValidationRunResponse.from(runService.runValidation(request.datasetVersionId(), principal));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DATASET_VIEW')")
    public ValidationRunResponse get(@PathVariable Long id) {
        return ValidationRunResponse.from(runService.get(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DATASET_VIEW')")
    public List<ValidationRunResponse> list(@RequestParam Long datasetVersionId) {
        return runService.listForVersion(datasetVersionId).stream().map(ValidationRunResponse::from).toList();
    }

    @GetMapping("/{id}/results")
    @PreAuthorize("hasAuthority('DATASET_VIEW')")
    public PageResponse<ValidationResultResponse> results(@PathVariable Long id,
                                                            @RequestParam(required = false) Boolean passed,
                                                            @RequestParam(required = false) RuleSeverity severity,
                                                            Pageable pageable) {
        return PageResponse.of(runService.results(id, passed, severity, pageable), ValidationResultResponse::from);
    }
}
