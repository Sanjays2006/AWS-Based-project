package com.cloudidentity.engine.rules;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class IamPolicyAnalyzer {

    public static class PolicyAnalysisResult {
        private final double riskScoreModifier;
        private final List<String> flaggedRules;

        public PolicyAnalysisResult(double riskScoreModifier, List<String> flaggedRules) {
            this.riskScoreModifier = riskScoreModifier;
            this.flaggedRules = flaggedRules;
        }

        public double getRiskScoreModifier() {
            return riskScoreModifier;
        }

        public List<String> getFlaggedRules() {
            return flaggedRules;
        }
    }

    public PolicyAnalysisResult analyzePolicyPermissions(String policyNameOrContent) {
        if (policyNameOrContent == null || policyNameOrContent.trim().isEmpty()) {
            return new PolicyAnalysisResult(0.0, List.of());
        }

        double penaltyScore = 0.0;
        List<String> flags = new ArrayList<>();
        String contentUpper = policyNameOrContent.toUpperCase();

        if (contentUpper.contains("ADMINISTRATORACCESS") || contentUpper.contains("ADMINISTRATOR")) {
            penaltyScore += 40.0;
            flags.add("High Privilege Policy Attached: AdministratorAccess (+40.0)");
        }

        if (contentUpper.contains("POWERUSERACCESS")) {
            penaltyScore += 25.0;
            flags.add("Elevated Privilege Policy Attached: PowerUserAccess (+25.0)");
        }

        if (contentUpper.contains("*:*") || contentUpper.contains("\"ACTION\": \"*\"")) {
            penaltyScore += 35.0;
            flags.add("Wildcard Action Detected: Action '*' permitted (+35.0)");
        }

        if (contentUpper.contains("IAM:ATTACHUSERPOLICY") || contentUpper.contains("IAM:CREATEROLE")) {
            penaltyScore += 20.0;
            flags.add("IAM Mutation Capability Detected (+20.0)");
        }

        if (flags.isEmpty()) {
            log.debug("IAM Policy Analyzer: Policy '{}' has clean least-privilege permissions.", policyNameOrContent);
        } else {
            log.warn("IAM Policy Analyzer: Flagged over-privileged policy '{}' with total penalty score: {}", policyNameOrContent, penaltyScore);
        }

        return new PolicyAnalysisResult(penaltyScore, flags);
    }
}
