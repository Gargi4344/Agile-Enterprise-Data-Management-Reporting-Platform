package com.aedmp.platform.dataset;

import com.aedmp.platform.common.web.PageResponse;
import com.aedmp.platform.dataset.dto.*;
import com.aedmp.platform.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;
    private final DatasetVersionService versionService;

    @GetMapping("/datasets")
    @PreAuthorize("hasAuthority('DATASET_VIEW')")
    public PageResponse<DatasetResponse> list(@RequestParam(required = false) String search, Pageable pageable) {
        return PageResponse.of(datasetService.list(search, pageable), DatasetResponse::from);
    }

    @PostMapping("/datasets")
    @PreAuthorize("hasAuthority('DATASET_UPLOAD')")
    @ResponseStatus(HttpStatus.CREATED)
    public DatasetResponse create(@Valid @RequestBody CreateDatasetRequest request,
                                   @AuthenticationPrincipal AuthenticatedUser principal) {
        return DatasetResponse.from(datasetService.create(request, principal));
    }

    @GetMapping("/datasets/{id}")
    @PreAuthorize("hasAuthority('DATASET_VIEW')")
    public DatasetResponse get(@PathVariable Long id) {
        return DatasetResponse.from(datasetService.get(id));
    }

    @PatchMapping("/datasets/{id}")
    @PreAuthorize("hasAuthority('DATASET_UPLOAD')")
    public DatasetResponse update(@PathVariable Long id, @RequestBody UpdateDatasetRequest request) {
        return DatasetResponse.from(datasetService.update(id, request));
    }

    @GetMapping("/datasets/{id}/versions")
    @PreAuthorize("hasAuthority('DATASET_VIEW')")
    public List<DatasetVersionResponse> versions(@PathVariable Long id) {
        return versionService.listForDataset(id).stream().map(DatasetVersionResponse::from).toList();
    }

    @PostMapping(value = "/datasets/{id}/versions", consumes = "multipart/form-data")
    @PreAuthorize("hasAuthority('DATASET_UPLOAD')")
    @ResponseStatus(HttpStatus.CREATED)
    public DatasetVersionResponse uploadVersion(@PathVariable Long id,
                                                 @RequestParam("file") MultipartFile file,
                                                 @RequestParam(value = "sourceSystem", required = false) String sourceSystem,
                                                 @AuthenticationPrincipal AuthenticatedUser principal) {
        return DatasetVersionResponse.from(versionService.upload(id, file, sourceSystem, principal));
    }

    @GetMapping("/versions/{id}")
    @PreAuthorize("hasAuthority('DATASET_VIEW')")
    public DatasetVersionResponse getVersion(@PathVariable Long id) {
        return DatasetVersionResponse.from(versionService.get(id));
    }

    @GetMapping("/versions/{id}/columns")
    @PreAuthorize("hasAuthority('DATASET_VIEW')")
    public List<DatasetColumnResponse> columns(@PathVariable Long id) {
        return versionService.columnsFor(id).stream().map(DatasetColumnResponse::from).toList();
    }
}
