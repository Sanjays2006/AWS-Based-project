package com.cloudidentity.service;

import com.cloudidentity.entity.RiskScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import java.time.Instant;

/**
 * Publishes platform risk metrics to Amazon CloudWatch under the
 * {@code CloudIdentity/Risk} namespace, so operators can build dashboards and
 * alarms (e.g. "alert when CRITICAL risk scores spike").
 *
 * <p>Resilient: when CloudWatch is unreachable or credentials/permissions are
 * missing, failures are logged at WARN and never propagate to the caller, so
 * metric publishing can never break the core risk-evaluation flow.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AwsCloudWatchMetricsService {

    private final CloudWatchClient cloudWatchClient;

    private static final String NAMESPACE = "CloudIdentity/Risk";

    /**
     * Publishes a single risk-score data point. The metric is tagged with the
     * identity ARN and risk level as dimensions.
     */
    public void publishRiskScore(RiskScore riskScore) {
        if (riskScore == null || riskScore.getIamIdentity() == null) {
            return;
        }
        try {
            String arn = riskScore.getIamIdentity().getArn();
            String level = riskScore.getRiskLevel() != null ? riskScore.getRiskLevel() : "UNKNOWN";
            double value = riskScore.getScore() != null ? riskScore.getScore().doubleValue() : 0.0;

            MetricDatum datum = MetricDatum.builder()
                    .metricName("RiskScore")
                    .unit(StandardUnit.NONE)
                    .value(value)
                    .timestamp(Instant.now())
                    .dimensions(
                            Dimension.builder().name("IdentityArn").value(arn != null ? arn : "unknown").build(),
                            Dimension.builder().name("RiskLevel").value(level).build())
                    .build();

            cloudWatchClient.putMetricData(PutMetricDataRequest.builder()
                    .namespace(NAMESPACE)
                    .metricData(datum)
                    .build());

            log.debug("[AWS CLOUDWATCH] Published RiskScore={} level={} for {}", value, level, arn);
        } catch (Exception ex) {
            log.warn("[AWS CLOUDWATCH] Failed to publish risk metric: {}", ex.getMessage());
        }
    }

    /**
     * Publishes a simple count metric (e.g. anomalies detected, findings
     * ingested) under the risk namespace.
     */
    public void publishCount(String metricName, double count) {
        try {
            MetricDatum datum = MetricDatum.builder()
                    .metricName(metricName)
                    .unit(StandardUnit.COUNT)
                    .value(count)
                    .timestamp(Instant.now())
                    .build();

            cloudWatchClient.putMetricData(PutMetricDataRequest.builder()
                    .namespace(NAMESPACE)
                    .metricData(datum)
                    .build());

            log.debug("[AWS CLOUDWATCH] Published {}={}", metricName, count);
        } catch (Exception ex) {
            log.warn("[AWS CLOUDWATCH] Failed to publish count metric '{}': {}", metricName, ex.getMessage());
        }
    }
}
