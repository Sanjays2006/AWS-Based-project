package com.cloudidentity.service;

import com.cloudidentity.repository.ActivityLogRepository;
import com.cloudidentity.repository.AnomalyEventRepository;
import com.cloudidentity.repository.IamIdentityRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CostDashboardService {

    private final ActivityLogRepository activityLogRepository;
    private final AnomalyEventRepository anomalyEventRepository;
    private final IamIdentityRepository iamIdentityRepository;

    @Data
    @Builder
    public static class FinOpsCostSummary {
        private long totalLogsIngested;
        private long totalMonitoredIdentities;
        private long totalAnomaliesEvaluated;
        private BigDecimal estimatedCloudTrailCostUsd;
        private BigDecimal estimatedLambdaCostUsd;
        private BigDecimal estimatedSnsNotificationCostUsd;
        private BigDecimal estimatedSecurityStorageCostUsd;
        private BigDecimal totalMonthlySecuritySpendUsd;
        private Map<String, BigDecimal> costBreakdownByService;
    }

    @Transactional(readOnly = true)
    public FinOpsCostSummary getCostSummary() {
        long totalLogs = activityLogRepository.count();
        long totalIdentities = iamIdentityRepository.countByIsMonitoredTrue();
        long totalAnomalies = anomalyEventRepository.count();

        // Estimated CloudTrail ingestion cost ($0.000002 per event)
        BigDecimal cloudTrailCost = BigDecimal.valueOf(totalLogs).multiply(BigDecimal.valueOf(0.000002))
                .setScale(2, RoundingMode.HALF_UP);

        // Estimated Lambda execution cost ($0.00001 per threat evaluation)
        BigDecimal lambdaCost = BigDecimal.valueOf(totalLogs).multiply(BigDecimal.valueOf(0.00001))
                .setScale(2, RoundingMode.HALF_UP);

        // Estimated SNS alert cost ($0.0005 per notification)
        BigDecimal snsCost = BigDecimal.valueOf(totalAnomalies).multiply(BigDecimal.valueOf(0.0005))
                .setScale(2, RoundingMode.HALF_UP);

        // Estimated security log storage ($0.023 per GB, baseline 5 GB)
        BigDecimal storageCost = BigDecimal.valueOf(5.0 * 0.023).setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalSpend = cloudTrailCost.add(lambdaCost).add(snsCost).add(storageCost);

        Map<String, BigDecimal> breakdown = new LinkedHashMap<>();
        breakdown.put("AWS CloudTrail Ingestion", cloudTrailCost);
        breakdown.put("AWS Lambda Compute", lambdaCost);
        breakdown.put("AWS SNS Alerts", snsCost);
        breakdown.put("AWS S3 Security Log Storage", storageCost);

        return FinOpsCostSummary.builder()
                .totalLogsIngested(totalLogs)
                .totalMonitoredIdentities(totalIdentities)
                .totalAnomaliesEvaluated(totalAnomalies)
                .estimatedCloudTrailCostUsd(cloudTrailCost)
                .estimatedLambdaCostUsd(lambdaCost)
                .estimatedSnsNotificationCostUsd(snsCost)
                .estimatedSecurityStorageCostUsd(storageCost)
                .totalMonthlySecuritySpendUsd(totalSpend)
                .costBreakdownByService(breakdown)
                .build();
    }
}
