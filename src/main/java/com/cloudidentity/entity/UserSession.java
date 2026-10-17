package com.cloudidentity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user", "device"})
@ToString(exclude = {"user", "device", "sessionToken"})
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_token", nullable = false, unique = true, length = 500)
    private String sessionToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "login_at", nullable = false)
    private LocalDateTime loginAt;

    @Column(name = "logout_at")
    private LocalDateTime logoutAt;

    @Column(name = "last_activity_at")
    private LocalDateTime lastActivityAt;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
