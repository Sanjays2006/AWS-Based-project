package com.cloudidentity.engine.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngineResult {
    private double anomalyConfidence; // 0.0 - 1.0
    private String decisionReason;
    
    @Builder.Default
    private List<Map<String, Object>> triggeredRules = new ArrayList<>();
    
    @Builder.Default
    private Map<String, Object> additionalMetrics = new HashMap<>();

    public void addTriggeredRule(String code, int weight, String detail) {
        triggeredRules.add(Map.of(
            "ruleCode", code,
            "weight", weight,
            "detail", detail
        ));
    }
}
