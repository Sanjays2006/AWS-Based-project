package com.cloudidentity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskScoreResponse {
    private Long id;
    private Long iamIdentityId;
    private String identityName;
    private String identityArn;
    private BigDecimal score;
    private String riskLevel;
    private List<Map<String, Object>> triggeredRules;
    private Map<String, Object> scoreFactors;
    private LocalDateTime computedAt;
}
