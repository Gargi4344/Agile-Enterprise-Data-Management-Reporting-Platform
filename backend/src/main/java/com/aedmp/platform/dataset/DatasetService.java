package com.aedmp.platform.dataset;

import com.aedmp.platform.audit.AuditResult;
import com.aedmp.platform.audit.AuditService;
import com.aedmp.platform.common.exception.BadRequestException;
import com.aedmp.platform.common.exception.ConflictException;
import com.aedmp.platform.common.exception.NotFoundException;
import com.aedmp.platform.dataset.dto.CreateDatasetRequest;
import com.aedmp.platform.dataset.dto.UpdateDatasetRequest;
import com.aedmp.platform.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DatasetService {

    private final DatasetRepository datasetRepository;
    private final AuditService auditService;

    public Page<Dataset> list(String search, Pageable pageable) {
        if (search == null || search.isBlank()) {
            return datasetRepository.findAll(pageable);
        }
        return datasetRepository.findByNameContainingIgnoreCase(search, pageable);
    }

    public Dataset get(Long id) {
        return datasetRepository.findById(id).orElseThrow(() -> new NotFoundException("Dataset " + id + " not found"));
    }

    @Transactional
    public Dataset create(CreateDatasetRequest request, AuthenticatedUser principal) {
        if (datasetRepository.existsByNameIgnoreCaseAndOwnerId(request.name(), principal.getId())) {
            throw new ConflictException("You already own a dataset named '" + request.name() + "'");
        }
        Dataset dataset = Dataset.builder()
                .name(request.name())
                .description(request.description())
                .owner(principal.getUser())
                .status(DatasetStatus.ACTIVE)
                .build();
        Dataset saved = datasetRepository.save(dataset);
        auditService.record("DATASET_CREATE", "DATASET", String.valueOf(saved.getId()), AuditResult.SUCCESS);
        return saved;
    }

    @Transactional
    public Dataset update(Long id, UpdateDatasetRequest request) {
        Dataset dataset = get(id);
        if (request.name() != null && !request.name().isBlank()) {
            dataset.setName(request.name());
        }
        if (request.description() != null) {
            dataset.setDescription(request.description());
        }
        if (request.status() != null) {
            try {
                dataset.setStatus(DatasetStatus.valueOf(request.status()));
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Unknown dataset status: " + request.status());
            }
        }
        Dataset saved = datasetRepository.save(dataset);
        auditService.record("DATASET_UPDATE", "DATASET", String.valueOf(saved.getId()), AuditResult.SUCCESS);
        return saved;
    }
}
