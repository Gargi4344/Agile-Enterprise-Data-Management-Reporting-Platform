package com.aedmp.platform.audit;

import com.aedmp.platform.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private User actor;

    @Column(name = "actor_email", nullable = false)
    private String actorEmail;

    @Column(nullable = false, length = 80)
    private String action;

    @Column(name = "target_type", nullable = false, length = 60)
    private String targetType;

    @Column(name = "target_id", length = 60)
    private String targetId;

    @Column(name = "request_id", length = 80)
    private String requestId;

    @Column(name = "job_id")
    private Long jobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditResult result;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
