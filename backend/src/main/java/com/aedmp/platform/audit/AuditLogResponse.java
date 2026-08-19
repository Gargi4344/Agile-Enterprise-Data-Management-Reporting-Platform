package com.aedmp.platform.audit;

import java.time.Instant;

public record AuditLogResponse(
        Long id,
        String actorEmail,
        String action,
        String targetType,
        String targetId,
        String requestId,
        Long jobId,
        String result,
        Object metadata,
        Instant createdAt
) {
    public static AuditLogResponse from(AuditLog a) {
        return new AuditLogResponse(a.getId(), a.getActorEmail(), a.getAction(), a.getTargetType(), a.getTargetId(),
                a.getRequestId(), a.getJobId(), a.getResult().name(), a.getMetadata(), a.getCreatedAt());
    }
}
