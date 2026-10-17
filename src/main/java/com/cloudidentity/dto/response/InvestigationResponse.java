package com.cloudidentity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestigationResponse {
    private IamIdentityResponse identity;
    private RiskScoreResponse currentRiskScore;
    private List<RiskRuleResponse> triggeredRules;
    private String riskExplanation;
    private List<String> recommendedActions;
    private List<ActivityLogResponse> recentCloudTrailEvents;
    private SessionResponse previousLogin;
    private SessionResponse currentLogin;
    private List<AlertTimelineResponse> activityTimeline;
    private List<AnomalyEventResponse> openAnomalies;
}
