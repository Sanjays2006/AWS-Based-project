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
public class SessionResponse {
    private Long id;
    private Long userId;
    private String ipAddress;
    private String browser;
    private String operatingSystem;
    private String deviceType;
    private LocalDateTime loginAt;
    private LocalDateTime logoutAt;
    private LocalDateTime lastActivityAt;
    private Integer durationMinutes;
    private boolean isActive;
}
