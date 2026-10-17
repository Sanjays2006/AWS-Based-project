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
public class IamIdentityResponse {
    private Long id;
    private String arn;
    private String identityType;
    private String accountId;
    private String identityName;
    private String region;
    private boolean isMonitored;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
