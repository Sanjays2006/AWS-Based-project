package com.cloudidentity.service;

import com.cloudidentity.dto.response.AnomalyEventResponse;
import com.cloudidentity.dto.response.DeviceResponse;
import com.cloudidentity.dto.response.PagedResponse;
import com.cloudidentity.dto.response.SessionResponse;
import com.cloudidentity.entity.*;
import com.cloudidentity.exception.ResourceNotFoundException;
import com.cloudidentity.repository.*;
import com.cloudidentity.util.MapperUtil;
import com.cloudidentity.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SelfServicePortalService {

    private final AnomalyEventRepository anomalyEventRepository;
    private final AlertNotificationRepository alertNotificationRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final IamIdentityRepository iamIdentityRepository;
    private final UserSessionRepository userSessionRepository;
    private final DeviceRepository deviceRepository;
    private final SessionService sessionService;
    private final MapperUtil mapperUtil;

    @Transactional(readOnly = true)
    public List<AnomalyEventResponse> getMyAlerts(User user) {
        // 1. Check notifications linked to the user
        List<AlertNotification> notes = alertNotificationRepository.findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(user.getId());
        List<AnomalyEvent> anomalies = notes.stream()
                .map(AlertNotification::getAnomalyEvent)
                .filter(event -> event != null && !Boolean.TRUE.equals(event.getIsAcknowledged()))
                .distinct()
                .collect(Collectors.toList());

        // 2. Also search if user matches an IamIdentity name or ARN
        List<IamIdentity> matchingIdentities = iamIdentityRepository.findAll().stream()
                .filter(i -> i.getIdentityName() != null && 
                             (i.getIdentityName().equalsIgnoreCase(user.getUsername()) || 
                              i.getArn().toLowerCase().contains(user.getUsername().toLowerCase())))
                .toList();

        for (IamIdentity identity : matchingIdentities) {
            List<AnomalyEvent> idAnomalies = anomalyEventRepository.findByIamIdentity_IdOrderByCreatedAtDesc(identity.getId());
            idAnomalies.stream()
                    .filter(event -> !Boolean.TRUE.equals(event.getIsAcknowledged()) && !anomalies.contains(event))
                    .forEach(anomalies::add);
        }

        return anomalies.stream().map(mapperUtil::toAnomalyEventResponse).collect(Collectors.toList());
    }

    @Transactional
    public void respondToAlert(Long alertId, User user, String response) {
        AnomalyEvent anomaly = anomalyEventRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Anomaly event not found"));

        anomaly.setIsAcknowledged(true);
        anomaly.setAcknowledgedByUser(user);
        anomaly.setAcknowledgedAt(LocalDateTime.now());
        anomaly.setAlertResponse(response);
        anomalyEventRepository.save(anomaly);

        IamIdentity identity = anomaly.getIamIdentity();

        if ("NOT_ME".equals(response)) {
            log.warn("CRITICAL ESCALATION: User {} reported alert ID {} as NOT ME!", user.getUsername(), alertId);
            
            // Generate a Critical risk score event
            RiskScore newScore = RiskScore.builder()
                    .iamIdentity(identity)
                    .score(BigDecimal.valueOf(100.00))
                    .riskLevel("CRITICAL")
                    .triggeredRules(List.of(Map.of(
                            "ruleCode", "NOT_ME_REPORT",
                            "weight", 50,
                            "detail", "Identity owner reported a detected anomaly alert as fraudulent/not them"
                    )))
                    .scoreFactors(Map.of("escalated", true, "reason", "Owner reported suspicious activity"))
                    .computedAt(LocalDateTime.now())
                    .build();
            riskScoreRepository.save(newScore);

        } else if ("WAS_ME".equals(response)) {
            log.info("User {} confirmed alert ID {} was indeed them. Refining risk scores.", user.getUsername(), alertId);
            
            // Lower risk score by inserting a updated score or modifying latest
            List<RiskScore> history = riskScoreRepository.findHistoryByIdentityId(identity.getId());
            if (!history.isEmpty()) {
                RiskScore latest = history.get(0);
                double scoreVal = Math.max(0.0, latest.getScore().doubleValue() - anomaly.getRiskDelta().doubleValue() - 10);
                
                RiskScore updatedScore = RiskScore.builder()
                        .iamIdentity(identity)
                        .score(BigDecimal.valueOf(scoreVal))
                        .riskLevel(scoreVal > 50 ? "HIGH" : (scoreVal > 25 ? "MEDIUM" : "LOW"))
                        .triggeredRules(Collections.emptyList())
                        .scoreFactors(Map.of("acknowledged", true, "reason", "User confirmed activity"))
                        .computedAt(LocalDateTime.now())
                        .build();
                riskScoreRepository.save(updatedScore);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> getMySessions(User user) {
        return userSessionRepository.findByUser_IdAndIsActiveTrue(user.getId()).stream()
                .map(mapperUtil::toSessionResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DeviceResponse> getMyDevices(User user) {
        return deviceRepository.findByUser_Id(user.getId()).stream()
                .map(mapperUtil::toDeviceResponse)
                .collect(Collectors.toList());
    }
}
