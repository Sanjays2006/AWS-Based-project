package com.cloudidentity.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskRuleRequest {

    @NotNull(message = "Weight is required")
    @Min(value = 0, message = "Weight cannot be less than 0")
    @Max(value = 100, message = "Weight cannot be greater than 100")
    private Integer weight;

    private Boolean isActive;
}
