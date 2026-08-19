package com.aedmp.platform.validation;

import com.aedmp.platform.security.AuthenticatedUser;
import com.aedmp.platform.validation.dto.CreateValidationRuleRequest;
import com.aedmp.platform.validation.dto.UpdateValidationRuleRequest;
import com.aedmp.platform.validation.dto.ValidationRuleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/validation/rules")
@RequiredArgsConstructor
public class ValidationRuleController {

    private final ValidationRuleService ruleService;

    @GetMapping
    @PreAuthorize("hasAuthority('DATASET_VIEW')")
    public List<ValidationRuleResponse> list(@RequestParam(required = false) Long datasetId) {
        return ruleService.listForDataset(datasetId).stream().map(ValidationRuleResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('RULE_MANAGE_ALL', 'RULE_MANAGE_OWN')")
    @ResponseStatus(HttpStatus.CREATED)
    public ValidationRuleResponse create(@Valid @RequestBody CreateValidationRuleRequest request,
                                          @AuthenticationPrincipal AuthenticatedUser principal) {
        return ValidationRuleResponse.from(ruleService.create(request, principal));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('RULE_MANAGE_ALL', 'RULE_MANAGE_OWN')")
    public ValidationRuleResponse update(@PathVariable Long id, @RequestBody UpdateValidationRuleRequest request,
                                          @AuthenticationPrincipal AuthenticatedUser principal) {
        return ValidationRuleResponse.from(ruleService.update(id, request, principal));
    }
}
