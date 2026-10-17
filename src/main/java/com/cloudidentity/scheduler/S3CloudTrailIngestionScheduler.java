package com.cloudidentity.scheduler;

import com.cloudidentity.config.AppProperties;
import com.cloudidentity.dto.response.IngestionResultResponse;
import com.cloudidentity.service.AwsS3CloudTrailReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Periodically ingests CloudTrail log files from S3 by delegating to the shared
 * {@link AwsS3CloudTrailReader#ingestRecentEvents()} service — the exact same
 * code path used by the manual admin trigger
 * ({@code POST /api/v1/scheduler/aws/s3-cloudtrail/trigger}). No ingestion logic
 * is duplicated here; this class only provides the schedule and the enable gate.
 *
 * <p><b>Disabled by default.</b> Activate via
 * {@code app.scheduler.s3-enabled=true} (env var {@code SCHEDULER_S3_ENABLED=true}),
 * which should only be turned on once AWS credentials and the CloudTrail S3
 * bucket ({@code aws.cloudtrail.s3-bucket}) are configured.
 *
 * <p>The poll interval is configurable via
 * {@code app.scheduler.s3-poll-ms} (default 300000 ms = 5 minutes).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3CloudTrailIngestionScheduler {

    private final AppProperties appProperties;
    private final AwsS3CloudTrailReader awsS3CloudTrailReader;

    /**
     * Scheduled poll. Uses {@code fixedDelayString} so the next run starts only
     * after the previous one finishes (no overlapping ingestion runs). An
     * initial delay prevents a run during application startup.
     */
    @Scheduled(
            fixedDelayString = "${app.scheduler.s3-poll-ms:300000}",
            initialDelayString = "${app.scheduler.s3-initial-delay-ms:60000}")
    public void pollAndIngestFromS3() {
        if (!appProperties.getScheduler().isS3Enabled()) {
            log.debug("S3 CloudTrail ingestion scheduler is disabled. Skipping.");
            return;
        }
        runIngestion("scheduled");
    }

    /** Manual trigger entry point — callable from the admin panel / scheduler controller. */
    public IngestionResultResponse triggerManually() {
        log.info("[SCHEDULER] Manual S3 CloudTrail ingestion triggered by administrator");
        return runIngestion("manual");
    }

    /**
     * Shared execution wrapper around the reused ingestion service, adding
     * status logging and defensive error handling so a failure never escapes
     * the scheduler thread.
     */
    private IngestionResultResponse runIngestion(String mode) {
        log.info("[SCHEDULER] Starting {} S3 CloudTrail ingestion run...", mode);
        try {
            IngestionResultResponse result = awsS3CloudTrailReader.ingestRecentEvents();
            if (!result.isSourceAvailable()) {
                log.warn("[SCHEDULER] S3 ingestion source unavailable: {}", result.getMessage());
            } else {
                log.info("[SCHEDULER] S3 ingestion ({}) complete: {}", mode, result.getMessage());
            }
            return result;
        } catch (Exception ex) {
            log.error("[SCHEDULER] S3 CloudTrail ingestion failed: {}", ex.getMessage(), ex);
            return IngestionResultResponse.builder()
                    .source("S3")
                    .sourceAvailable(false)
                    .message("S3 ingestion failed: " + ex.getMessage())
                    .build();
        }
    }
}
