package com.cloudidentity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "login_locations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user", "session"})
@ToString(exclude = {"user", "session"})
public class LoginLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private UserSession session;

    @Column(name = "ip_address", nullable = false, length = 50)
    private String ipAddress;

    @Column(name = "country_code", length = 10)
    private String countryCode;

    @Column(name = "country_name", length = 100)
    private String countryName;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "latitude", precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = 6)
    private BigDecimal longitude;

    @Builder.Default
    @Column(name = "is_vpn", nullable = false)
    private Boolean isVpn = false;

    @Builder.Default
    @Column(name = "is_tor", nullable = false)
    private Boolean isTor = false;

    @Column(name = "login_at")
    private LocalDateTime loginAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
