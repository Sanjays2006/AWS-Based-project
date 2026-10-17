package com.cloudidentity.service;

import com.cloudidentity.dto.response.*;
import com.cloudidentity.entity.AnomalyEvent;
import com.cloudidentity.entity.RiskScore;
import com.cloudidentity.repository.*;
import com.cloudidentity.util.MapperUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final IamIdentityRepository iamIdentityRepository;
    private final AnomalyEventRepository anomalyEventRepository;
    private final RiskScoreRepository riskScoreRepository;
    private final UserSessionRepository userSessionRepository;
    private final LoginLocationRepository loginLocationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final AutomatedResponseRepository automatedResponseRepository;
    private final MapperUtil mapperUtil;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        // 1. Get counts
        long totalUsers = userRepository.count();
        long totalMonitoredIdentities = iamIdentityRepository.countByIsMonitoredTrue();
        long totalAnomaliesDetected = anomalyEventRepository.count();
        long totalUnresolvedAlerts = anomalyEventRepository.countByIsAcknowledgedFalse();

        // Count risk levels from latest snapshot scores
        List<RiskScore> latestScores = riskScoreRepository.findLatestScoresForAllIdentities();
        long criticalRiskCount = 0;
        long highRiskCount = 0;
        long mediumRiskCount = 0;
        long lowRiskCount = 0;

        for (RiskScore rs : latestScores) {
            switch (rs.getRiskLevel()) {
                case "CRITICAL" -> criticalRiskCount++;
                case "HIGH" -> highRiskCount++;
                case "MEDIUM" -> mediumRiskCount++;
                default -> lowRiskCount++;
            }
        }

        // 2. Fetch Recent Anomalies (Top 5 unacknowledged)
        List<AnomalyEventResponse> recentAnomalies = anomalyEventRepository.findTop5ByIsAcknowledgedFalseOrderByCreatedAtDesc()
                .stream().map(mapperUtil::toAnomalyEventResponse).collect(Collectors.toList());

        // 3. Fetch Top Risk Identities (Top 5 scores DESC)
        List<RiskScoreResponse> topRiskIdentities = latestScores.stream()
                .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
                .limit(5)
                .map(mapperUtil::toRiskScoreResponse)
                .collect(Collectors.toList());

        // 4. Daily Threat Summary
        long anomaliesToday = anomalyEventRepository.countSince(todayStart);
        long criticalAlertsToday = anomalyEventRepository.countBySeveritySince("CRITICAL", todayStart);
        long autoResponsesToday = automatedResponseRepository.countByStatusSince("EXECUTED", todayStart);

        double averageRiskScoreToday = latestScores.isEmpty() ? 0.0 : latestScores.stream()
                .mapToDouble(s -> s.getScore().doubleValue())
                .average()
                .orElse(0.0);

        DashboardSummaryResponse.DailyThreatSummary dailyThreatSummary = DashboardSummaryResponse.DailyThreatSummary.builder()
                .anomaliesToday(anomaliesToday)
                .averageRiskScoreToday(averageRiskScoreToday)
                .criticalAlertsToday(criticalAlertsToday)
                .automatedResponsesExecutedToday(autoResponsesToday)
                .build();

        return DashboardSummaryResponse.builder()
                .totalUsers(totalUsers)
                .totalMonitoredIdentities(totalMonitoredIdentities)
                .totalAnomaliesDetected(totalAnomaliesDetected)
                .totalUnresolvedAlerts(totalUnresolvedAlerts)
                .criticalRiskCount(criticalRiskCount)
                .highRiskCount(highRiskCount)
                .mediumRiskCount(mediumRiskCount)
                .lowRiskCount(lowRiskCount)
                .recentAnomalies(recentAnomalies)
                .topRiskIdentities(topRiskIdentities)
                .dailyThreatSummary(dailyThreatSummary)
                .build();
    }

    @Transactional(readOnly = true)
    public HeatmapDataResponse getHeatmapData() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Object[]> queryResults = userSessionRepository.getHeatmapData(since);
        
        List<HeatmapDataResponse.HeatmapPoint> points = new ArrayList<>();
        for (Object[] row : queryResults) {
            Number dow = (Number) row[0];
            Number hr = (Number) row[1];
            Number count = (Number) row[2];
            
            if (dow != null && hr != null && count != null) {
                points.add(new HeatmapDataResponse.HeatmapPoint(
                        dow.intValue(), 
                        hr.intValue(), 
                        count.longValue()
                ));
            }
        }
        return new HeatmapDataResponse(points);
    }

    @Transactional(readOnly = true)
    public WorldMapDataResponse getWorldMapData() {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<Object[]> queryResults = loginLocationRepository.getGeographicMarkers(since);
        
        List<WorldMapDataResponse.LocationMarker> markers = new ArrayList<>();
        for (Object[] row : queryResults) {
            Number count = (Number) row[5];
            markers.add(WorldMapDataResponse.LocationMarker.builder()
                    .countryCode((String) row[0])
                    .countryName((String) row[1])
                    .city((String) row[2])
                    .latitude((BigDecimal) row[3])
                    .longitude((BigDecimal) row[4])
                    .count(count != null ? count.longValue() : 0L)
                    .hasAnomaly(Boolean.TRUE.equals(row[6]))
                    .build());
        }
        return new WorldMapDataResponse(markers);
    }

    @Transactional(readOnly = true)
    public RiskTrendResponse getRiskTrend(int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<RiskScore> scores = riskScoreRepository.findTrendData(since);

        // Group scores by date
        Map<LocalDate, List<RiskScore>> grouped = scores.stream()
                .collect(Collectors.groupingBy(s -> s.getComputedAt().toLocalDate()));

        List<RiskTrendResponse.RiskTrendPoint> points = new ArrayList<>();
        
        // Populate and sort
        for (Map.Entry<LocalDate, List<RiskScore>> entry : grouped.entrySet()) {
            List<RiskScore> dayScores = entry.getValue();
            double avg = dayScores.stream().mapToDouble(s -> s.getScore().doubleValue()).average().orElse(0.0);
            double max = dayScores.stream().mapToDouble(s -> s.getScore().doubleValue()).max().orElse(0.0);
            long critical = dayScores.stream().filter(s -> "CRITICAL".equals(s.getRiskLevel())).count();

            points.add(new RiskTrendResponse.RiskTrendPoint(entry.getKey(), avg, max, critical));
        }

        points.sort(Comparator.comparing(RiskTrendResponse.RiskTrendPoint::getDate));
        return new RiskTrendResponse(points);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getServiceUsage() {
        List<Object[]> queryResults = activityLogRepository.getAwsServiceUsageCounts();
        Map<String, Long> usage = new HashMap<>();
        for (Object[] row : queryResults) {
            String service = (String) row[0];
            Long count = (Long) row[1];
            if (service != null) {
                usage.put(service, count);
            }
        }
        return usage;
    }
}
