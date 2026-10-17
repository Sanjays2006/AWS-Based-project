package com.cloudidentity.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ResponsePolicyResponse {

    private Long id;
    private String policyName;
    private String riskLevelTrigger;
    private BigDecimal minRiskScore;
    private String responseAction;
    private String description;
    private Boolean isActive;
    private Integer priority;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
