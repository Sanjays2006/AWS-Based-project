package com.cloudidentity.service;

import com.cloudidentity.engine.BehaviorEngine;
import com.cloudidentity.engine.RiskEngine;
import com.cloudidentity.engine.RuleEngine;
import com.cloudidentity.engine.model.BehaviorVector;
import com.cloudidentity.engine.model.EngineResult;
import com.cloudidentity.entity.*;
import com.cloudidentity.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThreatDetectionService {

    private final BehaviorEngine behaviorEngine;
    private final RuleEngine ruleEngine;
    private final RiskEngine riskEngine;

    private final IamIdentityRepository iamIdentityRepository;
    private final BehaviorFingerprintRepository behaviorFingerprintRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final AnomalyEventRepository anomalyEventRepository;
    private final AutomatedResponseRepository automatedResponseRepository;
    private final ResponsePolicyRepository responsePolicyRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final SseEmitterService sseEmitterService;
    private final AwsCloudWatchMetricsService cloudWatchMetricsService;

    /**
     * End-to-end evaluation pipeline for an incoming CloudTrail ActivityLog event.
     */
    @Transactional
    public RiskScore evaluateActivityLog(ActivityLog activityLog) {
        if (activityLog == null || activityLog.getIamIdentity() == null) {
            log.debug("Skipping threat evaluation for log with missing IAM identity.");
            return null;
        }

        IamIdentity identity = activityLog.getIamIdentity();
        if (!Boolean.TRUE.equals(identity.getIsMonitored())) {
            log.debug("IAM Identity {} is not actively monitored.", identity.getArn());
            return null;
        }

        // 1. Extract vector
        BehaviorVector vector = behaviorEngine.extractVector(activityLog);

        // 2. Fetch baseline fingerprint
        BehaviorFingerprint fingerprint = behaviorFingerprintRepository
                .findByIamIdentity_Id(identity.getId())
                .orElse(null);

        // 3. Evaluate rules
        EngineResult engineResult = ruleEngine.evaluateRules(vector, fingerprint);

        // 4. Calculate & persist risk score
        RiskScore riskScore = riskEngine.calculateRisk(identity.getId(), engineResult);
        riskScore = riskScoreRepository.save(riskScore);

        // Publish the score to CloudWatch (no-op if AWS unconfigured).
        cloudWatchMetricsService.publishRiskScore(riskScore);

        // 5. Generate Anomaly & Notifications if rules triggered
        if (!engineResult.getTriggeredRules().isEmpty()) {
            createAnomalyAndAlerts(identity, activityLog, engineResult, riskScore);
        }

        return riskScore;
    }

    private void createAnomalyAndAlerts(IamIdentity identity, ActivityLog logItem, 
                                       EngineResult engineResult, RiskScore score) {
        
        String anomalyType = engineResult.getTriggeredRules().get(0).get("ruleCode").toString();
        String severity = score.getRiskLevel();

        AnomalyEvent anomaly = AnomalyEvent.builder()
                .iamIdentity(identity)
                .activityLog(logItem)
                .anomalyType(anomalyType)
                .severity(severity)
                .description(engineResult.getDecisionReason())
                .confidenceScore(BigDecimal.valueOf(engineResult.getAnomalyConfidence()))
                .riskDelta(score.getScore())
                .isAcknowledged(false)
                .createdAt(LocalDateTime.now())
                .build();

        anomaly = anomalyEventRepository.save(anomaly);
        log.warn("Generated AnomalyEvent [ID: {}, Severity: {}, Type: {}] for identity: {}", 
                anomaly.getId(), severity, anomalyType, identity.getIdentityName());

        // Broadcast live anomaly to connected SSE dashboard clients
        sseEmitterService.broadcastAnomaly(anomaly);

        // Notify admins & analysts
        List<User> notificationRecipients = userRepository.findAll().stream()
                .filter(User::getIsActive)
                .toList();

        for (User recipient : notificationRecipients) {
            notificationService.createNotification(
                    recipient,
                    anomaly,
                    "Security Threat Alert: " + severity,
                    String.format("Identity '%s' triggered %d security rule(s). Calculated Risk Score: %.1f",
                            identity.getIdentityName(), engineResult.getTriggeredRules().size(), score.getScore()),
                    "IN_APP"
            );
        }

        // Trigger Automated Mitigation driven by DB Response Policies
        triggerAutomatedMitigation(identity, anomaly, score);
    }

    // Trigger Automated Mitigation driven by DB Response Policies
    private void triggerAutomatedMitigation(IamIdentity identity, AnomalyEvent anomaly, RiskScore score) {
        List<ResponsePolicy> matchingPolicies = responsePolicyRepository.findMatchingPoliciesForScore(score.getScore());
        if (matchingPolicies.isEmpty()) {
            log.debug("No automated response policy triggered for score: {}", score.getScore());
            return;
        }

        ResponsePolicy activePolicy = matchingPolicies.get(0);
        AutomatedResponse autoResponse = AutomatedResponse.builder()
                .anomalyEvent(anomaly)
                .iamIdentity(identity)
                .responseType(activePolicy.getResponseAction())
                .status("EXECUTED")
                .executionDetails(Map.of(
                        "policyName", activePolicy.getPolicyName(),
                        "action", activePolicy.getResponseAction(),
                        "triggeredAt", LocalDateTime.now().toString(),
                        "reason", String.format("Risk score %.1f exceeded min threshold %.1f for policy '%s'",
                                score.getScore(), activePolicy.getMinRiskScore(), activePolicy.getPolicyName())
                ))
                .executedBy("SYSTEM_AUTOMATION")
                .executedAt(LocalDateTime.now())
                .build();

        automatedResponseRepository.save(autoResponse);
        log.info("[MITIGATION] Executed policy '{}' (Action: {}) for identity: {}", 
                activePolicy.getPolicyName(), activePolicy.getResponseAction(), identity.getArn());
    }
}
