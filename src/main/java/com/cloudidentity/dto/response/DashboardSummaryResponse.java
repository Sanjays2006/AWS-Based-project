package com.cloudidentity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private long totalUsers;
    private long totalMonitoredIdentities;
    private long totalAnomaliesDetected;
    private long totalUnresolvedAlerts;
    private long criticalRiskCount;
    private long highRiskCount;
    private long mediumRiskCount;
    private long lowRiskCount;

    private List<AnomalyEventResponse> recentAnomalies;
    private List<RiskScoreResponse> topRiskIdentities;
    private DailyThreatSummary dailyThreatSummary;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyThreatSummary {
        private long anomaliesToday;
        private double averageRiskScoreToday;
        private long criticalAlertsToday;
        private long automatedResponsesExecutedToday;
    }
}
