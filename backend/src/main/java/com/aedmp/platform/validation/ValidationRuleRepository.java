package com.aedmp.platform.validation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ValidationRuleRepository extends JpaRepository<ValidationRule, Long> {

    @Query("select r from ValidationRule r where r.active = true and (r.dataset.id = :datasetId or r.dataset is null)")
    List<ValidationRule> findActiveRulesForDataset(@Param("datasetId") Long datasetId);

    List<ValidationRule> findByDatasetIdOrderByCreatedAtDesc(Long datasetId);

    List<ValidationRule> findByDatasetIsNullOrderByCreatedAtDesc();
}
