package com.cloudidentity.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "behavior_fingerprints")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"iamIdentity"})
@ToString(exclude = {"iamIdentity"})
public class BehaviorFingerprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iam_identity_id", nullable = false, unique = true)
    private IamIdentity iamIdentity;

    // === Dimension 1: Time ===
    @Column(name = "typical_hours_start")
    private Integer typicalHoursStart;

    @Column(name = "typical_hours_end")
    private Integer typicalHoursEnd;

    /** Comma-separated weekday numbers: "1,2,3,4,5" = Mon-Fri */
    @Column(name = "typical_weekdays", length = 50)
    private String typicalWeekdays;

    // === Dimension 2: Location ===
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "typical_regions", columnDefinition = "jsonb")
    private List<String> typicalRegions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "typical_countries", columnDefinition = "jsonb")
    private List<String> typicalCountries;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "typical_ip_ranges", columnDefinition = "jsonb")
    private List<String> typicalIpRanges;

    // === Dimension 3: Device ===
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "typical_device_fingerprints", columnDefinition = "jsonb")
    private List<String> typicalDeviceFingerprints;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "typical_browsers", columnDefinition = "jsonb")
    private List<String> typicalBrowsers;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "typical_os_types", columnDefinition = "jsonb")
    private List<String> typicalOsTypes;

    // === Dimension 4: AWS Services ===
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "typical_services", columnDefinition = "jsonb")
    private List<String> typicalServices;

    // === Dimension 5: API Calls ===
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "typical_api_calls", columnDefinition = "jsonb")
    private List<String> typicalApiCalls;

    // === Dimension 6: Action Sequences ===
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "typical_resource_sequences", columnDefinition = "jsonb")
    private List<String> typicalResourceSequences;

    // === Dimension 7: Volume ===
    @Builder.Default
    @Column(name = "avg_daily_api_calls", precision = 10, scale = 2)
    private BigDecimal avgDailyApiCalls = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "avg_data_download_bytes")
    private Long avgDataDownloadBytes = 0L;

    @Builder.Default
    @Column(name = "max_data_download_bytes")
    private Long maxDataDownloadBytes = 0L;

    // === Dimension 8: Session ===
    @Builder.Default
    @Column(name = "avg_session_duration_minutes", precision = 10, scale = 2)
    private BigDecimal avgSessionDurationMinutes = BigDecimal.ZERO;

    // === Dimension 9: Login Frequency ===
    @Builder.Default
    @Column(name = "avg_logins_per_day", precision = 5, scale = 2)
    private BigDecimal avgLoginsPerDay = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "avg_failed_logins_per_day", precision = 5, scale = 2)
    private BigDecimal avgFailedLoginsPerDay = BigDecimal.ZERO;

    // === Dimension 10: Privilege ===
    @Builder.Default
    @Column(name = "avg_privilege_escalations_per_week", precision = 5, scale = 2)
    private BigDecimal avgPrivilegeEscalationsPerWeek = BigDecimal.ZERO;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "known_high_privilege_actions", columnDefinition = "jsonb")
    private List<String> knownHighPrivilegeActions;

    // === Metadata ===
    @Column(name = "baseline_computed_at")
    private LocalDateTime baselineComputedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
