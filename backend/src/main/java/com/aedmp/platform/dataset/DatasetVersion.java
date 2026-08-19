package com.aedmp.platform.dataset;

import com.aedmp.platform.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "dataset_versions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatasetVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id", nullable = false)
    private Dataset dataset;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "source_filename", nullable = false)
    private String sourceFilename;

    @Column(name = "source_system", nullable = false)
    @Builder.Default
    private String sourceSystem = "manual-upload";

    @Column(name = "checksum_sha256", nullable = false, length = 64)
    private String checksumSha256;

    @Column(name = "storage_location", nullable = false)
    private String storageLocation;

    @Column(name = "file_size_bytes", nullable = false)
    @Builder.Default
    private Long fileSizeBytes = 0L;

    @Column(name = "row_count")
    private Long rowCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DatasetVersionStatus status = DatasetVersionStatus.UPLOADED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by", nullable = false)
    private User uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = createdAt;
        if (uploadedAt == null) {
            uploadedAt = createdAt;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }
}
