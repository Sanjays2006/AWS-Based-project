package com.cloudidentity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceResponse {
    private Long id;
    private String deviceFingerprint;
    private String browser;
    private String operatingSystem;
    private String deviceType;
    private boolean isTrusted;
    private LocalDateTime firstSeenAt;
    private LocalDateTime lastSeenAt;
}
