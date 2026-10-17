package com.cloudidentity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ResponsePolicyRequest {

    @NotBlank(message = "Policy name is required")
    private String policyName;

    @NotBlank(message = "Risk level trigger is required (LOW, MEDIUM, HIGH, CRITICAL)")
    private String riskLevelTrigger;

    @NotNull(message = "Minimum risk score is required")
    private BigDecimal minRiskScore;

    @NotBlank(message = "Response action is required")
    private String responseAction;

    private String description;

    private Boolean isActive = true;

    private Integer priority = 1;
}
