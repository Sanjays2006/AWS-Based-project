package com.cloudidentity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "aws_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AwsAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 12-digit AWS Account ID, e.g. 123456789012 */
    @Column(name = "account_id", nullable = false, unique = true, length = 20)
    private String accountId;

    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    @Column(name = "account_alias", length = 100)
    private String accountAlias;

    @Builder.Default
    @Column(name = "region", nullable = false, length = 50)
    private String region = "us-east-1";

    @Column(name = "access_key_id", length = 100)
    private String accessKeyId;

    @Column(name = "secret_access_key", length = 200)
    private String secretAccessKey;

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
