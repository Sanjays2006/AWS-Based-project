package com.cloudidentity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "response_policies")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponsePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_name", nullable = false, length = 100)
    private String policyName;

    /** LOW | MEDIUM | HIGH | CRITICAL */
    @Column(name = "risk_level_trigger", nullable = false, length = 50)
    private String riskLevelTrigger;

    /** Minimum risk score to trigger this policy, e.g. 80.0 */
    @Column(name = "min_risk_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal minRiskScore;

    /** DENY_POLICY | QUARANTINE | REVOKE_SESSION | REQUIRE_MFA | NOTIFY_ONLY | DISABLE_USER */
    @Column(name = "response_action", nullable = false, length = 100)
    private String responseAction;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "priority", nullable = false)
    private Integer priority = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
