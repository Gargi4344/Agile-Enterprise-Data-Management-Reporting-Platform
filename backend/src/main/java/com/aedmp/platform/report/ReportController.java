package com.aedmp.platform.report;

import com.aedmp.platform.report.dto.CreateReportRequest;
import com.aedmp.platform.report.dto.KpiOverviewResponse;
import com.aedmp.platform.report.dto.ReportResponse;
import com.aedmp.platform.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('REPORT_VIEW')")
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public List<ReportResponse> list(@RequestParam Long datasetId) {
        return reportService.listForDataset(datasetId).stream().map(ReportResponse::from).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportResponse create(@Valid @RequestBody CreateReportRequest request, @AuthenticationPrincipal AuthenticatedUser principal) {
        return ReportResponse.from(reportService.generate(request.datasetVersionId(), request.reportType(), principal));
    }

    @GetMapping("/{id}")
    public ReportResponse get(@PathVariable Long id) {
        return ReportResponse.from(reportService.get(id));
    }

    @GetMapping("/kpis/overview")
    public KpiOverviewResponse kpiOverview() {
        return reportService.kpiOverview();
    }
}
