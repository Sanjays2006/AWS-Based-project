package com.cloudidentity.service;

import com.cloudidentity.dto.response.BehavioralDriftResponse;
import com.cloudidentity.dto.response.TopRiskyApiResponse;
import com.cloudidentity.entity.BehaviorHistory;
import com.cloudidentity.repository.ActivityLogRepository;
import com.cloudidentity.repository.AnomalyEventRepository;
import com.cloudidentity.repository.BehaviorHistoryRepository;
import com.cloudidentity.repository.RiskScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final BehaviorHistoryRepository behaviorHistoryRepository;
    private final ActivityLogRepository activityLogRepository;
    private final AnomalyEventRepository anomalyEventRepository;
    private final RiskScoreRepository riskScoreRepository;

    @Transactional(readOnly = true)
    public List<BehavioralDriftResponse> getBehavioralDriftByIdentity(Long identityId, Integer days) {
        int lookbackDays = (days != null && days > 0) ? days : 30;
        LocalDate sinceDate = LocalDate.now().minusDays(lookbackDays);

        List<BehaviorHistory> history = behaviorHistoryRepository
                .findByIamIdentity_IdAndSnapshotDateGreaterThanEqualOrderBySnapshotDateAsc(identityId, sinceDate);

        return history.stream()
                .map(h -> BehavioralDriftResponse.builder()
                        .snapshotDate(h.getSnapshotDate())
                        .apiCallCount(h.getApiCallCount())
                        .loginCount(h.getLoginCount())
                        .uniqueRegions(h.getUniqueRegions())
                        .privilegeEscalations(h.getPrivilegeEscalations())
                        .dataDownloadBytes(h.getDataDownloadBytes())
                        .riskScoreEod(h.getRiskScoreEod())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TopRiskyApiResponse> getTopRiskyApis(Integer limit) {
        int max = (limit != null && limit > 0) ? limit : 10;
        LocalDateTime since = LocalDateTime.now().minusDays(30);

        // Aggregate top event names triggering security events
        List<TopRiskyApiResponse> results = activityLogRepository.findAll().stream()
                .filter(l -> l.getEventName() != null)
                .collect(Collectors.groupingBy(
                        l -> l.getAwsService() + ":" + l.getEventName(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(max)
                .map(entry -> {
                    String[] parts = entry.getKey().split(":");
                    String service = parts[0];
                    String action = parts[1];
                    String cat = action.toLowerCase().contains("policy") || action.toLowerCase().contains("key") 
                            ? "PRIVILEGE" : "BEHAVIOR";
                    return TopRiskyApiResponse.builder()
                            .awsService(service)
                            .apiCallName(action)
                            .triggerCount(entry.getValue())
                            .riskCategory(cat)
                            .description("Triggered " + entry.getValue() + " CloudTrail monitored events")
                            .build();
                })
                .collect(Collectors.toList());

        return results;
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getRiskDistribution() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        long critical = riskScoreRepository.countByRiskLevelSince("CRITICAL", since);
        long high = riskScoreRepository.countByRiskLevelSince("HIGH", since);
        long medium = riskScoreRepository.countByRiskLevelSince("MEDIUM", since);
        long low = riskScoreRepository.countByRiskLevelSince("LOW", since);

        return Map.of(
                "CRITICAL", critical,
                "HIGH", high,
                "MEDIUM", medium,
                "LOW", low
        );
    }
}
