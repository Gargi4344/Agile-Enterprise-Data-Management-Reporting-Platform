package com.aedmp.platform.validation;

import com.aedmp.platform.audit.AuditResult;
import com.aedmp.platform.audit.AuditService;
import com.aedmp.platform.common.exception.BadRequestException;
import com.aedmp.platform.common.exception.NotFoundException;
import com.aedmp.platform.dataset.Dataset;
import com.aedmp.platform.dataset.DatasetRepository;
import com.aedmp.platform.security.AuthenticatedUser;
import com.aedmp.platform.validation.dto.CreateValidationRuleRequest;
import com.aedmp.platform.validation.dto.UpdateValidationRuleRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ValidationRuleService {

    private final ValidationRuleRepository ruleRepository;
    private final DatasetRepository datasetRepository;
    private final AuditService auditService;

    public List<ValidationRule> listForDataset(Long datasetId) {
        if (datasetId == null) {
            return ruleRepository.findByDatasetIsNullOrderByCreatedAtDesc();
        }
        return ruleRepository.findActiveRulesForDataset(datasetId);
    }

    @Transactional
    public ValidationRule create(CreateValidationRuleRequest request, AuthenticatedUser principal) {
        Dataset dataset = null;
        if (request.datasetId() != null) {
            dataset = datasetRepository.findById(request.datasetId())
                    .orElseThrow(() -> new NotFoundException("Dataset " + request.datasetId() + " not found"));
        } else if (!principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("RULE_MANAGE_ALL"))) {
            throw new AccessDeniedException("Only admins may create global (dataset-less) rules");
        }

        ValidationRule rule = ValidationRule.builder()
                .name(request.name())
                .description(request.description())
                .ruleFamily(parseEnum(RuleFamily.class, request.ruleFamily(), "ruleFamily"))
                .severity(request.severity() != null ? parseEnum(RuleSeverity.class, request.severity(), "severity") : RuleSeverity.BLOCKER)
                .dataset(dataset)
                .columnName(request.columnName())
                .expressionType(parseEnum(ExpressionType.class, request.expressionType(), "expressionType"))
                .expression(request.expression() != null ? request.expression() : "")
                .active(true)
                .weight(request.weight() != null ? request.weight() : BigDecimal.ONE)
                .owner(principal.getUser())
                .version(1)
                .build();
        ValidationRule saved = ruleRepository.save(rule);
        auditService.record("VALIDATION_RULE_CREATE", "VALIDATION_RULE", String.valueOf(saved.getId()), AuditResult.SUCCESS);
        return saved;
    }

    @Transactional
    public ValidationRule update(Long id, UpdateValidationRuleRequest request, AuthenticatedUser principal) {
        ValidationRule rule = ruleRepository.findById(id).orElseThrow(() -> new NotFoundException("Rule " + id + " not found"));

        boolean canManageAll = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("RULE_MANAGE_ALL"));
        boolean isOwner = rule.getOwner().getId().equals(principal.getId());
        if (!canManageAll && !isOwner) {
            auditService.record("VALIDATION_RULE_UPDATE", "VALIDATION_RULE", String.valueOf(id), AuditResult.DENIED);
            throw new AccessDeniedException("You may only edit validation rules you own");
        }

        if (request.name() != null) rule.setName(request.name());
        if (request.description() != null) rule.setDescription(request.description());
        if (request.severity() != null) rule.setSeverity(parseEnum(RuleSeverity.class, request.severity(), "severity"));
        if (request.columnName() != null) rule.setColumnName(request.columnName());
        if (request.expressionType() != null) rule.setExpressionType(parseEnum(ExpressionType.class, request.expressionType(), "expressionType"));
        if (request.expression() != null) rule.setExpression(request.expression());
        if (request.active() != null) rule.setActive(request.active());
        if (request.weight() != null) rule.setWeight(request.weight());
        rule.setVersion(rule.getVersion() + 1);

        ValidationRule saved = ruleRepository.save(rule);
        auditService.record("VALIDATION_RULE_UPDATE", "VALIDATION_RULE", String.valueOf(saved.getId()), AuditResult.SUCCESS);
        return saved;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> type, String value, String field) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid " + field + ": " + value);
        }
    }
}
