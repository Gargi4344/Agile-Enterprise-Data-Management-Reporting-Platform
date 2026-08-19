package com.aedmp.platform.dataset;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DatasetColumnRepository extends JpaRepository<DatasetColumn, Long> {

    List<DatasetColumn> findByDatasetVersionIdOrderByOrdinalPositionAsc(Long datasetVersionId);
}
