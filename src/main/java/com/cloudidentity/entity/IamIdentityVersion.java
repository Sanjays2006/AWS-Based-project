package com.cloudidentity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "iam_identity_versions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IamIdentityVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iam_identity_id", nullable = false)
    private IamIdentity iamIdentity;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "identity_name", nullable = false, length = 128)
    private String identityName;

    @Column(name = "attached_policies", columnDefinition = "TEXT")
    private String attachedPolicies;

    @Column(name = "risk_weight_modifier")
    private Double riskWeightModifier;

    @Column(name = "changed_by", length = 100)
    private String changedBy;

    @Column(name = "change_reason", length = 500)
    private String changeReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
