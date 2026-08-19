package com.aedmp.platform.dataset;

import com.aedmp.platform.audit.AuditResult;
import com.aedmp.platform.audit.AuditService;
import com.aedmp.platform.common.exception.BadRequestException;
import com.aedmp.platform.common.exception.ConflictException;
import com.aedmp.platform.common.exception.NotFoundException;
import com.aedmp.platform.dataset.csv.CsvDatasetReader;
import com.aedmp.platform.dataset.csv.CsvTable;
import com.aedmp.platform.dataset.profiling.ProfiledSchema;
import com.aedmp.platform.dataset.profiling.SchemaProfilingService;
import com.aedmp.platform.dataset.storage.FileStorageService;
import com.aedmp.platform.dataset.storage.StoredFile;
import com.aedmp.platform.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DatasetVersionService {

    private final DatasetRepository datasetRepository;
    private final DatasetVersionRepository versionRepository;
    private final DatasetColumnRepository columnRepository;
    private final FileStorageService fileStorageService;
    private final CsvDatasetReader csvDatasetReader;
    private final SchemaProfilingService schemaProfilingService;
    private final AuditService auditService;

    public List<DatasetVersion> listForDataset(Long datasetId) {
        return versionRepository.findByDatasetIdOrderByVersionNumberDesc(datasetId);
    }

    public DatasetVersion get(Long id) {
        return versionRepository.findById(id).orElseThrow(() -> new NotFoundException("Dataset version " + id + " not found"));
    }

    public List<DatasetColumn> columnsFor(Long versionId) {
        return columnRepository.findByDatasetVersionIdOrderByOrdinalPositionAsc(versionId);
    }

    @Transactional
    public DatasetVersion upload(Long datasetId, MultipartFile file, String sourceSystem, AuthenticatedUser principal) {
        Dataset dataset = datasetRepository.findById(datasetId)
                .orElseThrow(() -> new NotFoundException("Dataset " + datasetId + " not found"));
        if (dataset.getStatus() == DatasetStatus.ARCHIVED) {
            throw new BadRequestException("Cannot upload a version to an archived dataset");
        }
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("MISSING_FILE", "A file is required");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new BadRequestException("MALFORMED_FILE", "Could not read uploaded file");
        }

        int nextVersion = versionRepository.findFirstByDatasetIdOrderByVersionNumberDesc(datasetId)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        StoredFile stored = storeBytes(dataset, nextVersion, file, bytes);

        versionRepository.findByDatasetIdAndChecksumSha256(datasetId, stored.checksumSha256()).ifPresent(existing -> {
            throw new ConflictException("This exact file was already uploaded as version " + existing.getVersionNumber()
                    + ". Upload a changed file, or use the existing version.");
        });

        CsvTable table = csvDatasetReader.read(new java.io.ByteArrayInputStream(bytes));
        ProfiledSchema profile = schemaProfilingService.profile(table);

        DatasetVersion version = DatasetVersion.builder()
                .dataset(dataset)
                .versionNumber(nextVersion)
                .sourceFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload.csv")
                .sourceSystem(sourceSystem == null || sourceSystem.isBlank() ? "manual-upload" : sourceSystem)
                .checksumSha256(stored.checksumSha256())
                .storageLocation(stored.location())
                .fileSizeBytes(stored.sizeBytes())
                .rowCount(profile.rowCount())
                .status(DatasetVersionStatus.UPLOADED)
                .uploadedBy(principal.getUser())
                .uploadedAt(Instant.now())
                .build();
        DatasetVersion savedVersion = versionRepository.save(version);

        profile.columns().forEach(pc -> columnRepository.save(DatasetColumn.builder()
                .datasetVersion(savedVersion)
                .columnName(pc.columnName())
                .expectedType(null)
                .observedType(pc.observedType())
                .ordinalPosition(pc.ordinalPosition())
                .nullableObserved(pc.nullable())
                .distinctCount(pc.distinctCount())
                .nullCount(pc.nullCount())
                .sampleValues(pc.sampleValues())
                .build()));

        auditService.record("DATASET_VERSION_UPLOAD", "DATASET_VERSION", String.valueOf(savedVersion.getId()),
                AuditResult.SUCCESS, null, Map.of("datasetId", datasetId, "versionNumber", nextVersion,
                        "rowCount", profile.rowCount(), "checksum", stored.checksumSha256()));

        return savedVersion;
    }

    private StoredFile storeBytes(Dataset dataset, int versionNumber, MultipartFile file, byte[] bytes) {
        try (InputStream in = new java.io.ByteArrayInputStream(bytes)) {
            return fileStorageService.store(String.valueOf(dataset.getId()), "v" + versionNumber,
                    file.getOriginalFilename(), in);
        } catch (IOException e) {
            throw new BadRequestException("STORAGE_FAILURE", "Could not store uploaded file: " + e.getMessage());
        }
    }

    @Transactional
    public DatasetVersion transition(Long versionId, DatasetVersionStatus target) {
        DatasetVersion version = get(versionId);
        if (!version.getStatus().canTransitionTo(target)) {
            throw new ConflictException("Cannot transition dataset version " + versionId + " from "
                    + version.getStatus() + " to " + target);
        }
        version.setStatus(target);
        return versionRepository.save(version);
    }
}
