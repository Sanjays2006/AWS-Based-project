package com.cloudidentity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginLocationResponse {
    private Long id;
    private String ipAddress;
    private String countryCode;
    private String countryName;
    private String city;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean isVpn;
    private boolean isTor;
    private LocalDateTime loginAt;
}
