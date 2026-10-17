package com.cloudidentity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BehaviorFingerprintResponse {
    private Long id;
    private Long iamIdentityId;
    private String identityName;
    private String identityArn;

    private Integer typicalHoursStart;
    private Integer typicalHoursEnd;
    private String typicalWeekdays;

    private List<String> typicalRegions;
    private List<String> typicalCountries;
    private List<String> typicalIpRanges;

    private List<String> typicalDeviceFingerprints;
    private List<String> typicalBrowsers;
    private List<String> typicalOsTypes;

    private List<String> typicalServices;
    private List<String> typicalApiCalls;
    private List<String> typicalResourceSequences;

    private BigDecimal avgDailyApiCalls;
    private Long avgDataDownloadBytes;
    private Long maxDataDownloadBytes;
    private BigDecimal avgSessionDurationMinutes;

    private BigDecimal avgLoginsPerDay;
    private BigDecimal avgFailedLoginsPerDay;

    private BigDecimal avgPrivilegeEscalationsPerWeek;
    private List<String> knownHighPrivilegeActions;

    private LocalDateTime baselineComputedAt;
    private LocalDateTime updatedAt;
}
