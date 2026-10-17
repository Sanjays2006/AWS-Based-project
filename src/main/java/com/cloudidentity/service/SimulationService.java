package com.cloudidentity.service;

import com.cloudidentity.entity.ActivityLog;
import com.cloudidentity.entity.IamIdentity;
import com.cloudidentity.entity.RiskScore;
import com.cloudidentity.exception.ResourceNotFoundException;
import com.cloudidentity.repository.IamIdentityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimulationService {

    private final IamIdentityRepository iamIdentityRepository;
    private final ThreatDetectionService threatDetectionService;

    public List<Map<String, Object>> getAvailableScenarios() {
        return List.of(
                Map.of(
                        "code", "NEW_COUNTRY_ATTACK",
                        "name", "Unusual Geographic Login",
                        "description", "Simulates an IAM user logging in from Moscow, Russia (RU) at 3:15 AM.",
                        "targetRule", "NEW_COUNTRY + MIDNIGHT_LOGIN",
                        "expectedSeverity", "HIGH"
                ),
                Map.of(
                        "code", "PRIVILEGE_ESCALATION_ATTACK",
                        "name", "IAM Privilege Escalation Burst",
                        "description", "Simulates attaching AdministratorAccess IAM policy and creating a secondary access key.",
                        "targetRule", "PRIVILEGE_ESCALATION + IAM_POLICY_CHANGE",
                        "expectedSeverity", "CRITICAL"
                ),
                Map.of(
                        "code", "MASS_DOWNLOAD_ATTACK",
                        "name", "Massive S3 Data Exfiltration",
                        "description", "Simulates downloading 500 MB of sensitive data in a single S3 GetObject call.",
                        "targetRule", "MASS_DOWNLOAD",
                        "expectedSeverity", "HIGH"
                ),
                Map.of(
                        "code", "IMPOSSIBLE_TRAVEL_ATTACK",
                        "name", "Impossible Travel (India -> Japan in 10 mins)",
                        "description", "Simulates consecutive logins from Mumbai, India and Tokyo, Japan 10 minutes apart.",
                        "targetRule", "IMPOSSIBLE_TRAVEL",
                        "expectedSeverity", "CRITICAL"
                ),
                Map.of(
                        "code", "BRUTE_FORCE_ATTACK",
                        "name", "Tor Exit Node Stealth Attack",
                        "description", "Simulates console activity routed through a known TOR anonymizing exit node.",
                        "targetRule", "VPN_OR_TOR + MIDNIGHT_LOGIN",
                        "expectedSeverity", "HIGH"
                )
        );
    }

    public Map<String, Object> runSimulation(String scenarioCode, Long identityId) {
        IamIdentity identity;
        if (identityId != null) {
            identity = iamIdentityRepository.findById(identityId)
                    .orElseThrow(() -> new ResourceNotFoundException("IAM Identity not found with ID: " + identityId));
        } else {
            identity = iamIdentityRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No monitored IAM identity available to run simulation."));
        }

        log.info("[SIMULATION] Running attack scenario '{}' against identity '{}'", scenarioCode, identity.getIdentityName());

        ActivityLog simulatedLog = createSimulatedLog(scenarioCode, identity);
        RiskScore score = threatDetectionService.evaluateActivityLog(simulatedLog);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("scenario", scenarioCode);
        result.put("identity", identity.getIdentityName());
        result.put("identityArn", identity.getArn());
        result.put("simulatedIp", simulatedLog.getSourceIp());
        result.put("eventName", simulatedLog.getEventName());
        result.put("calculatedRiskScore", score != null ? score.getScore() : 0.0);
        result.put("riskLevel", score != null ? score.getRiskLevel() : "LOW");
        result.put("triggeredRulesCount", score != null && score.getTriggeredRules() != null ? score.getTriggeredRules().size() : 0);
        result.put("triggeredRules", score != null ? score.getTriggeredRules() : List.of());
        result.put("status", "COMPLETED");
        result.put("timestamp", LocalDateTime.now().toString());

        return result;
    }

    private ActivityLog createSimulatedLog(String scenarioCode, IamIdentity identity) {
        LocalDateTime now = LocalDateTime.now();

        switch (scenarioCode) {
            case "NEW_COUNTRY_ATTACK":
                return ActivityLog.builder()
                        .iamIdentity(identity)
                        .eventName("ConsoleLogin")
                        .eventSource("signin.amazonaws.com")
                        .eventTime(now.withHour(3).withMinute(15)) // 3:15 AM
                        .sourceIp("185.220.101.5") // Russian IP range
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0")
                        .region("us-east-1")
                        .awsService("signin.amazonaws.com")
                        .dataVolumeBytes(0L)
                        .rawEvent(Map.of("simulated", true, "scenario", scenarioCode))
                        .build();

            case "PRIVILEGE_ESCALATION_ATTACK":
                return ActivityLog.builder()
                        .iamIdentity(identity)
                        .eventName("AttachUserPolicy")
                        .eventSource("iam.amazonaws.com")
                        .eventTime(now)
                        .sourceIp("198.51.100.42")
                        .userAgent("aws-cli/2.15.0 Python/3.11.6")
                        .region("us-east-1")
                        .awsService("iam")
                        .requestParameters(Map.of("policyArn", "arn:aws:iam::aws:policy/AdministratorAccess"))
                        .dataVolumeBytes(0L)
                        .rawEvent(Map.of("simulated", true, "scenario", scenarioCode))
                        .build();

            case "MASS_DOWNLOAD_ATTACK":
                return ActivityLog.builder()
                        .iamIdentity(identity)
                        .eventName("GetObject")
                        .eventSource("s3.amazonaws.com")
                        .eventTime(now)
                        .sourceIp("203.0.113.88")
                        .userAgent("aws-cli/2.15.0 Python/3.11.6")
                        .region("us-west-2")
                        .awsService("s3")
                        .dataVolumeBytes(524288000L) // 500 MB download
                        .rawEvent(Map.of("simulated", true, "scenario", scenarioCode))
                        .build();

            case "IMPOSSIBLE_TRAVEL_ATTACK":
                return ActivityLog.builder()
                        .iamIdentity(identity)
                        .eventName("ConsoleLogin")
                        .eventSource("signin.amazonaws.com")
                        .eventTime(now)
                        .sourceIp("133.242.0.1") // Tokyo, Japan IP
                        .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)")
                        .region("ap-northeast-1")
                        .awsService("signin.amazonaws.com")
                        .dataVolumeBytes(0L)
                        .rawEvent(Map.of("simulated", true, "scenario", scenarioCode))
                        .build();

            case "BRUTE_FORCE_ATTACK":
            default:
                return ActivityLog.builder()
                        .iamIdentity(identity)
                        .eventName("ConsoleLogin")
                        .eventSource("signin.amazonaws.com")
                        .eventTime(now.withHour(2).withMinute(45))
                        .sourceIp("185.220.101.4") // Tor Exit Node IP
                        .userAgent("Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/115.0")
                        .region("us-east-1")
                        .awsService("signin.amazonaws.com")
                        .dataVolumeBytes(0L)
                        .rawEvent(Map.of("simulated", true, "scenario", scenarioCode))
                        .build();
        }
    }
}
