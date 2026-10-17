package com.cloudidentity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "devices")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user"})
@ToString(exclude = {"user"})
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "device_fingerprint", nullable = false, length = 500)
    private String deviceFingerprint;

    @Column(name = "browser", length = 100)
    private String browser;

    @Column(name = "operating_system", length = 100)
    private String operatingSystem;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "screen_resolution", length = 30)
    private String screenResolution;

    @Column(name = "timezone", length = 100)
    private String timezone;

    @Builder.Default
    @Column(name = "is_trusted", nullable = false)
    private Boolean isTrusted = false;

    @Column(name = "first_seen_at")
    private LocalDateTime firstSeenAt;

    @Column(name = "last_seen_at")
    private LocalDateTime lastSeenAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
