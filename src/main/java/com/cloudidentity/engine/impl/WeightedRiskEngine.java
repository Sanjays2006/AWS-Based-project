package com.cloudidentity.engine.impl;

import com.cloudidentity.config.AppProperties;
import com.cloudidentity.engine.RiskEngine;
import com.cloudidentity.engine.model.EngineResult;
import com.cloudidentity.entity.IamIdentity;
import com.cloudidentity.entity.RiskScore;
import com.cloudidentity.repository.IamIdentityRepository;
import com.cloudidentity.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeightedRiskEngine implements RiskEngine {

    private final IamIdentityRepository iamIdentityRepository;
    private final AppProperties appProperties;

    @Override
    public RiskScore calculateRisk(Long identityId, EngineResult ruleResult) {
        IamIdentity identity = iamIdentityRepository.findById(identityId)
                .orElseThrow(() -> new ResourceNotFoundException("IAM Identity not found with id: " + identityId));

        // 1. Calculate Risk Score = Σ(weights)
        int sumOfWeights = ruleResult.getTriggeredRules().stream()
                .mapToInt(rule -> (Integer) rule.get("weight"))
                .sum();

        // Cap at 100.00
        BigDecimal score = BigDecimal.valueOf(Math.min(sumOfWeights, 100))
                .setScale(2, RoundingMode.HALF_UP);

        // 2. Map risk level based on thresholds
        String level = mapRiskLevel(score.doubleValue());

        // 3. Assemble factors
        Map<String, Object> factors = new HashMap<>();
        factors.put("triggeredCount", ruleResult.getTriggeredRules().size());
        factors.put("sumOfWeights", sumOfWeights);
        factors.put("anomalyConfidence", ruleResult.getAnomalyConfidence());
        factors.put("decisionReason", ruleResult.getDecisionReason());

        log.info("Identity '{}' evaluated: Raw Score Sum = {}, Final Risk Level = {}", 
                identity.getIdentityName(), sumOfWeights, level);

        return RiskScore.builder()
                .iamIdentity(identity)
                .score(score)
                .riskLevel(level)
                .triggeredRules(ruleResult.getTriggeredRules())
                .scoreFactors(factors)
                .computedAt(LocalDateTime.now())
                .build();
    }

    private String mapRiskLevel(double scoreValue) {
        if (scoreValue >= appProperties.getRisk().getCriticalThreshold()) {
            return "CRITICAL";
        } else if (scoreValue >= appProperties.getRisk().getHighThreshold()) {
            return "HIGH";
        } else if (scoreValue >= appProperties.getRisk().getMediumThreshold()) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}
