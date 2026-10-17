package com.cloudidentity.engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BehaviorVector {
    private String identityArn;
    private LocalDateTime eventTime;
    private String region;
    private String countryCode;
    private String sourceIp;
    private String userAgent;
    private String browser;
    private String os;
    private String deviceType;
    private String deviceFingerprint;
    private String awsService;
    private String apiCallName;
    private long dataVolumeBytes;
    
    // Extracted session specifics
    private int loginHour;
    private int loginDayOfWeek; // 1 (Mon) - 7 (Sun)
    private boolean isVpn;
    private boolean isTor;
    private boolean isWeekend;
    private boolean isMidnight;
}
