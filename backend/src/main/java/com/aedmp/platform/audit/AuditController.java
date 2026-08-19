package com.aedmp.platform.audit;

import com.aedmp.platform.common.web.PageResponse;
import com.aedmp.platform.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AUDIT_VIEW_ALL sees every audit event. AUDIT_VIEW_LIMITED (Manager role) is scoped to events the
 * user themselves triggered - a conservative reading of "limited" scope from docs/rbac-matrix.md
 * that avoids the larger job of joining across dataset/report ownership for v1.
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('AUDIT_VIEW_ALL', 'AUDIT_VIEW_LIMITED')")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    public PageResponse<AuditLogResponse> list(@RequestParam(required = false) String targetType,
                                                @RequestParam(required = false) String targetId,
                                                @RequestParam(required = false) Long actorId,
                                                @AuthenticationPrincipal AuthenticatedUser principal,
                                                Pageable pageable) {
        boolean viewAll = principal.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("AUDIT_VIEW_ALL"));
        Long effectiveActorId = viewAll ? actorId : principal.getId();

        Specification<AuditLog> spec = Specification.where(null);
        if (targetType != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("targetType"), targetType));
        }
        if (targetId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("targetId"), targetId));
        }
        if (effectiveActorId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("actor").get("id"), effectiveActorId));
        }

        return PageResponse.of(auditLogRepository.findAll(spec, pageable), AuditLogResponse::from);
    }
}
