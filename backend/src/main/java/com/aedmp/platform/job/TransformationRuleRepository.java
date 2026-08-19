package com.aedmp.platform.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransformationRuleRepository extends JpaRepository<TransformationRule, Long> {

    List<TransformationRule> findByDatasetIdAndActiveTrueOrderByOrdinalPositionAsc(Long datasetId);
}
