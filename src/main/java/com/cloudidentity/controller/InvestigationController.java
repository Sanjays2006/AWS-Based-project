package com.cloudidentity.controller;

import com.cloudidentity.dto.response.*;
import com.cloudidentity.entity.*;
import com.cloudidentity.exception.ResourceNotFoundException;
import com.cloudidentity.repository.*;
import com.cloudidentity.util.MapperUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/investigation")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
public class InvestigationController {

    private final IamIdentityRepository iamIdentityRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final RiskRuleRepository riskRuleRepository;
    private final ActivityLogRepository activityLogRepository;
    private final AnomalyEventRepository anomalyEventRepository;
    private final UserSessionRepository userSessionRepository;
    private final MapperUtil mapperUtil;

    @GetMapping("/{identityId}")
    public ResponseEntity<InvestigationResponse> investigate(@PathVariable Long identityId) {
        IamIdentity identity = iamIdentityRepository.findById(identityId)
                .orElseThrow(() -> new ResourceNotFoundException("IAM Identity not found: " + identityId));

        // 1. Current risk score
        List<RiskScore> scoreHistory = riskScoreRepository.findHistoryByIdentityId(identityId);
        RiskScore currentScore = scoreHistory.isEmpty() ? null : scoreHistory.get(0);
        RiskScoreResponse currentRiskResponse = mapperUtil.toRiskScoreResponse(currentScore);

        // 2. Triggered rules
        List<RiskRuleResponse> triggeredRules = new ArrayList<>();
        if (currentScore != null && currentScore.getTriggeredRules() != null) {
            currentScore.getTriggeredRules().forEach(ruleMap -> {
                String ruleCode = (String) ruleMap.get("ruleCode");
                riskRuleRepository.findByRuleCode(ruleCode).ifPresent(rule ->
                        triggeredRules.add(mapperUtil.toRiskRuleResponse(rule)));
            });
        }

        // 3. Risk explanation
        String explanation = buildRiskExplanation(currentScore, triggeredRules);

        // 4. Recommended actions
        List<String> recommendations = buildRecommendations(currentScore, triggeredRules);

        // 5. Recent CloudTrail events
        List<ActivityLogResponse> recentEvents = activityLogRepository
                .findTop10ByIamIdentity_IdOrderByEventTimeDesc(identityId)
                .stream().map(mapperUtil::toActivityLogResponse).collect(Collectors.toList());

        // 6. Open anomaly events
        List<AnomalyEventResponse> openAnomalies = anomalyEventRepository
                .findByIamIdentity_IdOrderByCreatedAtDesc(identityId)
                .stream()
                .filter(e -> !e.getIsAcknowledged())
                .map(mapperUtil::toAnomalyEventResponse)
                .collect(Collectors.toList());

        // 7. Build timeline
        List<AlertTimelineResponse> timeline = new ArrayList<>();
        anomalyEventRepository.findByIamIdentity_IdOrderByCreatedAtDesc(identityId).forEach(event ->
                timeline.add(AlertTimelineResponse.builder()
                        .id(event.getId())
                        .type("ANOMALY")
                        .title(event.getAnomalyType())
                        .description(event.getDescription())
                        .severity(event.getSeverity())
                        .timestamp(event.getCreatedAt())
                        .referenceId(event.getId())
                        .build()));

        return ResponseEntity.ok(InvestigationResponse.builder()
                .identity(mapperUtil.toIamIdentityResponse(identity))
                .currentRiskScore(currentRiskResponse)
                .triggeredRules(triggeredRules)
                .riskExplanation(explanation)
                .recommendedActions(recommendations)
                .recentCloudTrailEvents(recentEvents)
                .openAnomalies(openAnomalies)
                .activityTimeline(timeline)
                .build());
    }

    private String buildRiskExplanation(RiskScore score, List<RiskRuleResponse> rules) {
        if (score == null) return "No risk score data available yet. Baseline not established.";
        String level = score.getRiskLevel();
        int count = rules.size();
        return String.format("Risk Level: %s (Score: %.1f). %d security rule(s) triggered. " +
                "The weighted sum of triggered rule weights equals the final risk score.",
                level, score.getScore(), count);
    }

    private List<String> buildRecommendations(RiskScore score, List<RiskRuleResponse> rules) {
        List<String> recommendations = new ArrayList<>();
        if (score == null) return List.of("Collect activity logs to establish baseline behavior.");

        switch (score.getRiskLevel()) {
            case "CRITICAL" -> {
                recommendations.add("Immediately revoke all active sessions for this identity.");
                recommendations.add("Attach an IAM Deny-All policy to halt access temporarily.");
                recommendations.add("Initiate a full security audit of the account activity.");
                recommendations.add("Contact the identity owner to confirm account validity.");
            }
            case "HIGH" -> {
                recommendations.add("Review and disable high-privilege API permissions.");
                recommendations.add("Enforce MFA authentication for this identity.");
                recommendations.add("Monitor all future activities with real-time alerting.");
            }
            case "MEDIUM" -> {
                recommendations.add("Review login source locations and verify recent activities.");
                recommendations.add("Confirm device fingerprints with the identity owner.");
            }
            default -> {
                recommendations.add("No immediate action required. Continue monitoring.");
            }
        }

        for (RiskRuleResponse rule : rules) {
            if ("PRIVILEGE".equals(rule.getCategory())) {
                recommendations.add("Audit IAM permission set — privilege escalation activity detected.");
            }
            if ("LOCATION".equals(rule.getCategory())) {
                recommendations.add("Review login origins for geographic impossibility indicators.");
            }
        }

        return recommendations;
    }
}
