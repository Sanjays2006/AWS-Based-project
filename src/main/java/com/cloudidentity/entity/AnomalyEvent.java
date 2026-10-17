package com.cloudidentity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "anomaly_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"iamIdentity", "activityLog", "acknowledgedByUser"})
@ToString(exclude = {"iamIdentity", "activityLog", "acknowledgedByUser"})
public class AnomalyEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iam_identity_id", nullable = false)
    private IamIdentity iamIdentity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_log_id")
    private ActivityLog activityLog;

    @Column(name = "anomaly_type", nullable = false, length = 100)
    private String anomalyType;

    /** LOW | MEDIUM | HIGH | CRITICAL */
    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Builder.Default
    @Column(name = "risk_delta", precision = 5, scale = 2)
    private BigDecimal riskDelta = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "is_acknowledged", nullable = false)
    private Boolean isAcknowledged = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by")
    private User acknowledgedByUser;

    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;

    /** WAS_ME | NOT_ME | IGNORED */
    @Column(name = "alert_response", length = 20)
    private String alertResponse;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
