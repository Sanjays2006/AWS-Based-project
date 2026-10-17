package com.cloudidentity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "automated_responses")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"anomalyEvent", "iamIdentity"})
@ToString(exclude = {"anomalyEvent", "iamIdentity", "executionDetails"})
public class AutomatedResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anomaly_event_id")
    private AnomalyEvent anomalyEvent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iam_identity_id")
    private IamIdentity iamIdentity;

    /** DENY_POLICY | QUARANTINE | REVOKE_SESSION | ALERT_ONLY */
    @Column(name = "response_type", nullable = false, length = 100)
    private String responseType;

    /** PENDING | EXECUTED | FAILED | ROLLED_BACK */
    @Builder.Default
    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "execution_details", columnDefinition = "jsonb")
    private Map<String, Object> executionDetails;

    @Column(name = "executed_by", length = 100)
    private String executedBy;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
