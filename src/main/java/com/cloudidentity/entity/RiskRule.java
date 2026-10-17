package com.cloudidentity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "risk_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Unique rule code, e.g. NEW_COUNTRY, PRIVILEGE_ESCALATION */
    @Column(name = "rule_code", nullable = false, unique = true, length = 100)
    private String ruleCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Weight contribution to risk score (0–50) */
    @Column(name = "weight", nullable = false)
    private Integer weight;

    /** LOCATION | DEVICE | TIME | BEHAVIOR | PRIVILEGE */
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
