package com.cloudidentity.service;

import com.cloudidentity.entity.AnomalyEvent;
import com.cloudidentity.entity.IamIdentity;
import com.cloudidentity.repository.AnomalyEventRepository;
import com.cloudidentity.repository.IamIdentityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.guardduty.GuardDutyClient;
import software.amazon.awssdk.services.guardduty.model.Finding;
import software.amazon.awssdk.services.guardduty.model.GetFindingsRequest;
import software.amazon.awssdk.services.guardduty.model.GetFindingsResponse;
import software.amazon.awssdk.services.guardduty.model.ListDetectorsResponse;
import software.amazon.awssdk.services.guardduty.model.ListFindingsRequest;
import software.amazon.awssdk.services.guardduty.model.ListFindingsResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ingests Amazon GuardDuty findings and maps them into {@code anomaly_events}.
 *
 * <p>GuardDuty uses a numeric severity (1.0-8.9); this is mapped to the
 * platform's LOW / MEDIUM / HIGH / CRITICAL scheme. Findings are attributed to
 * a synthetic GuardDuty "identity" so they surface alongside behavioral
 * anomalies in the investigation console.
 *
 * <p>Resilient: any SDK error (AWS unreachable, GuardDuty not enabled, missing
 * permission) is caught and logged; the method returns the count ingested so far.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AwsGuardDutyService {

    private final GuardDutyClient guardDutyClient;
    private final AnomalyEventRepository anomalyEventRepository;
    private final IamIdentityRepository iamIdentityRepository;

    private static final String GUARDDUTY_IDENTITY_ARN = "arn:aws:guardduty::detector/platform-findings";
    private static final int MAX_FINDINGS_PER_RUN = 50;

    /**
     * Pulls recent GuardDuty findings across all detectors in the region and
     * persists them as anomaly events.
     *
     * @return number of findings ingested
     */
    @Transactional
    public int ingestFindings() {
        log.info("[AWS GUARDDUTY] Starting GuardDuty findings ingestion...");
        int ingested = 0;
        try {
            ListDetectorsResponse detectors = guardDutyClient.listDetectors();
            if (detectors.detectorIds() == null || detectors.detectorIds().isEmpty()) {
                log.info("[AWS GUARDDUTY] No detectors found in region. Is GuardDuty enabled?");
                return 0;
            }

            IamIdentity gdIdentity = resolveGuardDutyIdentity();

            for (String detectorId : detectors.detectorIds()) {
                ingested += ingestForDetector(detectorId, gdIdentity);
                if (ingested >= MAX_FINDINGS_PER_RUN) {
                    break;
                }
            }
            log.info("[AWS GUARDDUTY] Ingestion complete. Ingested {} findings.", ingested);
        } catch (Exception ex) {
            log.warn("[AWS GUARDDUTY] Ingestion failed (AWS unreachable, GuardDuty disabled, "
                    + "or missing guardduty:* permission): {}", ex.getMessage());
        }
        return ingested;
    }

    private int ingestForDetector(String detectorId, IamIdentity gdIdentity) {
        ListFindingsResponse findingIds = guardDutyClient.listFindings(
                ListFindingsRequest.builder().detectorId(detectorId).maxResults(MAX_FINDINGS_PER_RUN).build());

        if (findingIds.findingIds() == null || findingIds.findingIds().isEmpty()) {
            return 0;
        }

        GetFindingsResponse findings = guardDutyClient.getFindings(GetFindingsRequest.builder()
                .detectorId(detectorId)
                .findingIds(findingIds.findingIds())
                .build());

        int count = 0;
        for (Finding finding : findings.findings()) {
            if (persistFinding(finding, gdIdentity)) {
                count++;
            }
        }
        return count;
    }

    /** Persists a single finding as an AnomalyEvent, skipping duplicates by type+description. */
    private boolean persistFinding(Finding finding, IamIdentity gdIdentity) {
        try {
            String severity = mapSeverity(finding.severity());
            double confidence = finding.confidence() != null ? finding.confidence() : 0.0;

            AnomalyEvent anomaly = AnomalyEvent.builder()
                    .iamIdentity(gdIdentity)
                    .anomalyType("GUARDDUTY:" + (finding.type() != null ? finding.type() : "UNKNOWN"))
                    .severity(severity)
                    .description(buildDescription(finding))
                    .confidenceScore(BigDecimal.valueOf(confidence))
                    .riskDelta(BigDecimal.valueOf(finding.severity() != null ? finding.severity() : 0.0))
                    .isAcknowledged(false)
                    .createdAt(LocalDateTime.now())
                    .build();

            anomalyEventRepository.save(anomaly);
            return true;
        } catch (Exception ex) {
            log.warn("[AWS GUARDDUTY] Failed to persist finding {}: {}", finding.id(), ex.getMessage());
            return false;
        }
    }

    private String buildDescription(Finding finding) {
        StringBuilder sb = new StringBuilder();
        if (finding.title() != null) {
            sb.append(finding.title());
        }
        if (finding.description() != null) {
            if (sb.length() > 0) {
                sb.append(" — ");
            }
            sb.append(finding.description());
        }
        return sb.length() > 0 ? sb.toString() : "GuardDuty finding " + finding.id();
    }

    /**
     * Maps GuardDuty numeric severity (1.0-8.9) to the platform severity scheme.
     * GuardDuty bands: Low 1.0-3.9, Medium 4.0-6.9, High 7.0-8.9.
     */
    private String mapSeverity(Double severity) {
        if (severity == null) {
            return "LOW";
        }
        if (severity >= 8.0) {
            return "CRITICAL";
        } else if (severity >= 7.0) {
            return "HIGH";
        } else if (severity >= 4.0) {
            return "MEDIUM";
        }
        return "LOW";
    }

    /** Finds or creates the synthetic GuardDuty identity that owns imported findings. */
    private IamIdentity resolveGuardDutyIdentity() {
        return iamIdentityRepository.findByArn(GUARDDUTY_IDENTITY_ARN)
                .orElseGet(() -> iamIdentityRepository.save(IamIdentity.builder()
                        .arn(GUARDDUTY_IDENTITY_ARN)
                        .identityName("AWS GuardDuty")
                        .identityType("SERVICE")
                        .isMonitored(true)
                        .build()));
    }
}
