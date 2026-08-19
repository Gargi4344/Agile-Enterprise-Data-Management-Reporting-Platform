package com.aedmp.platform.dataset;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DatasetRepository extends JpaRepository<Dataset, Long> {

    Page<Dataset> findByNameContainingIgnoreCase(String search, Pageable pageable);

    boolean existsByNameIgnoreCaseAndOwnerId(String name, Long ownerId);

    long countByCreatedAtAfter(java.time.Instant since);
}
