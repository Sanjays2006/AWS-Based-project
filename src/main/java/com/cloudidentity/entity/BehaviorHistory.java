package com.cloudidentity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "behavior_history",
       uniqueConstraints = @UniqueConstraint(columnNames = {"iam_identity_id", "snapshot_date"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"iamIdentity"})
@ToString(exclude = {"iamIdentity"})
public class BehaviorHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iam_identity_id", nullable = false)
    private IamIdentity iamIdentity;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Builder.Default @Column(name = "api_call_count", nullable = false)
    private Integer apiCallCount = 0;

    @Builder.Default @Column(name = "login_count", nullable = false)
    private Integer loginCount = 0;

    @Builder.Default @Column(name = "failed_login_count", nullable = false)
    private Integer failedLoginCount = 0;

    @Builder.Default @Column(name = "unique_regions", nullable = false)
    private Integer uniqueRegions = 0;

    @Builder.Default @Column(name = "unique_devices", nullable = false)
    private Integer uniqueDevices = 0;

    @Builder.Default @Column(name = "privilege_escalations", nullable = false)
    private Integer privilegeEscalations = 0;

    @Builder.Default @Column(name = "data_download_bytes", nullable = false)
    private Long dataDownloadBytes = 0L;

    @Builder.Default @Column(name = "session_count", nullable = false)
    private Integer sessionCount = 0;

    @Column(name = "avg_session_minutes", precision = 10, scale = 2)
    private BigDecimal avgSessionMinutes;

    @Column(name = "risk_score_eod", precision = 5, scale = 2)
    private BigDecimal riskScoreEod;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
