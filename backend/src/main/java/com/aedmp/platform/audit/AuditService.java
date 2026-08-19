package com.aedmp.platform.audit;

import com.aedmp.platform.security.AuthenticatedUser;
import com.aedmp.platform.common.web.RequestIdFilter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Runs in its own transaction (REQUIRES_NEW) so an audit write always survives even when the
     * caller's transaction later rolls back on a FAILURE/DENIED outcome - the audit trail must
     * reflect what was attempted, not only what succeeded.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String targetType, String targetId, AuditResult result, Long jobId,
                        Map<String, Object> metadata) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        AuditLog.AuditLogBuilder builder = AuditLog.builder()
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .result(result)
                .jobId(jobId)
                .requestId(MDC.get(RequestIdFilter.MDC_KEY))
                .metadata(toJson(metadata));

        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser principal) {
            builder.actor(principal.getUser()).actorEmail(principal.getUsername());
        } else {
            builder.actorEmail("system");
        }
        auditLogRepository.save(builder.build());
    }

    public void record(String action, String targetType, String targetId, AuditResult result) {
        record(action, targetType, targetId, result, null, Map.of());
    }

    private JsonNode toJson(Map<String, Object> metadata) {
        ObjectNode node = objectMapper.createObjectNode();
        if (metadata != null) {
            metadata.forEach((k, v) -> node.putPOJO(k, v));
        }
        return node;
    }
}
