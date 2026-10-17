package com.cloudidentity.controller;

import com.cloudidentity.scheduler.BaselineRetrainingScheduler;
import com.cloudidentity.scheduler.CloudTrailIngestionScheduler;
import com.cloudidentity.scheduler.DailyReportScheduler;
import com.cloudidentity.scheduler.FingerprintRecomputeScheduler;
import com.cloudidentity.scheduler.S3CloudTrailIngestionScheduler;
import com.cloudidentity.dto.response.IngestionResultResponse;
import com.cloudidentity.service.AwsGuardDutyService;
import com.cloudidentity.service.AwsIamSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/scheduler")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SchedulerController {

    private final CloudTrailIngestionScheduler cloudTrailIngestionScheduler;
    private final FingerprintRecomputeScheduler fingerprintRecomputeScheduler;
    private final DailyReportScheduler dailyReportScheduler;
    private final BaselineRetrainingScheduler baselineRetrainingScheduler;
    private final AwsIamSyncService awsIamSyncService;
    private final AwsGuardDutyService awsGuardDutyService;
    private final S3CloudTrailIngestionScheduler s3CloudTrailIngestionScheduler;

    @PostMapping("/cloudtrail/trigger")
    public ResponseEntity<Map<String, String>> triggerCloudTrailIngestion() {
        cloudTrailIngestionScheduler.triggerManually();
        return ResponseEntity.ok(Map.of("message", "CloudTrail ingestion scheduler triggered manually"));
    }

    @PostMapping("/fingerprint/trigger")
    public ResponseEntity<Map<String, String>> triggerFingerprintRecompute() {
        fingerprintRecomputeScheduler.triggerManually();
        return ResponseEntity.ok(Map.of("message", "Fingerprint recomputation scheduler triggered manually"));
    }

    @PostMapping("/daily-report/trigger")
    public ResponseEntity<Map<String, String>> triggerDailyReport() {
        dailyReportScheduler.triggerManually();
        return ResponseEntity.ok(Map.of("message", "Daily report scheduler triggered manually"));
    }

    @PostMapping("/baseline/trigger")
    public ResponseEntity<Map<String, String>> triggerBaselineRetraining() {
        baselineRetrainingScheduler.triggerManually();
        return ResponseEntity.ok(Map.of("message", "ML Baseline retraining scheduler triggered manually"));
    }

    @PostMapping("/aws/iam-sync/trigger")
    public ResponseEntity<Map<String, Object>> triggerIamSync() {
        int synced = awsIamSyncService.syncIdentities();
        return ResponseEntity.ok(Map.of(
                "message", "AWS IAM identity sync triggered manually",
                "identitiesSynced", synced));
    }

    @PostMapping("/aws/guardduty/trigger")
    public ResponseEntity<Map<String, Object>> triggerGuardDutyIngestion() {
        int ingested = awsGuardDutyService.ingestFindings();
        return ResponseEntity.ok(Map.of(
                "message", "AWS GuardDuty findings ingestion triggered manually",
                "findingsIngested", ingested));
    }

    /**
     * Reads CloudTrail log files from S3, de-duplicates, persists, and runs each
     * event through the threat-detection pipeline. ADMIN-only (class-level
     * @PreAuthorize + Spring Security rule on /api/v1/scheduler/**).
     */
    @PostMapping("/aws/s3-cloudtrail/trigger")
    public ResponseEntity<IngestionResultResponse> triggerS3CloudTrailIngestion() {
        return ResponseEntity.ok(s3CloudTrailIngestionScheduler.triggerManually());
    }
}
