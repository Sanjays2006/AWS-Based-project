package com.cloudidentity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "risk_scores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"iamIdentity"})
@ToString(exclude = {"iamIdentity"})
public class RiskScore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iam_identity_id", nullable = false)
    private IamIdentity iamIdentity;

    /** Computed risk score: 0.00 – 100.00 */
    @Column(name = "score", nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    /** LOW | MEDIUM | HIGH | CRITICAL */
    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel;

    /** JSON array: [{ruleCode, weight, detail}] */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "triggered_rules", columnDefinition = "jsonb")
    private List<Map<String, Object>> triggeredRules;

    /** Full factor breakdown JSON */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "score_factors", columnDefinition = "jsonb")
    private Map<String, Object> scoreFactors;

    @CreationTimestamp
    @Column(name = "computed_at", nullable = false, updatable = false)
    private LocalDateTime computedAt;
}
