package com.cloudidentity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "iam_identities")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IamIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Full AWS ARN, e.g. arn:aws:iam::123456789012:user/john */
    @Column(name = "arn", nullable = false, unique = true, length = 500)
    private String arn;

    /** USER | ROLE | GROUP */
    @Column(name = "identity_type", length = 50)
    private String identityType;

    @Column(name = "account_id", length = 20)
    private String accountId;

    @Column(name = "identity_name", length = 200)
    private String identityName;

    @Column(name = "region", length = 50)
    private String region;

    @Builder.Default
    @Column(name = "is_monitored", nullable = false)
    private Boolean isMonitored = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
