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
public class AnomalyEventResponse {
    private Long id;
    private Long iamIdentityId;
    private String identityName;
    private String identityArn;
    private Long activityLogId;
    private String anomalyType;
    private String severity;
    private String description;
    private BigDecimal confidenceScore;
    private BigDecimal riskDelta;
    private boolean isAcknowledged;
    private Long acknowledgedByUserId;
    private String acknowledgedByUsername;
    private LocalDateTime acknowledgedAt;
    private String alertResponse; // WAS_ME | NOT_ME | null
    private LocalDateTime createdAt;
}
