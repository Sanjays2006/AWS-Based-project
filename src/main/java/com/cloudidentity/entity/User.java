package com.cloudidentity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"roles"})
@ToString(exclude = {"passwordHash", "mfaSecret", "roles"})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", length = 200)
    private String fullName;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "mfa_enabled", nullable = false)
    private Boolean mfaEnabled = false;

    @Column(name = "mfa_secret", length = 100)
    private String mfaSecret;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Builder.Default
    @Column(name = "failed_attempt_count", nullable = false)
    private Integer failedAttemptCount = 0;

    @Builder.Default
    @Column(name = "account_non_locked", nullable = false)
    private Boolean accountNonLocked = true;

    @Column(name = "lockout_time")
    private LocalDateTime lockoutTime;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
