package com.cloudidentity.scheduler;

import com.cloudidentity.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Sunday at 2:00 AM — triggers full ML baseline retraining.
 * In production: Would call a SageMaker training job or push data to ML pipeline.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BaselineRetrainingScheduler {

    private final AppProperties appProperties;

    @Scheduled(cron = "0 0 2 * * SUN") // Sunday 2AM
    public void retrainBaselines() {
        if (!appProperties.getScheduler().isBaselineEnabled()) {
            log.debug("Baseline retraining scheduler is disabled. Skipping.");
            return;
        }

        log.info("[SCHEDULER] Starting weekly ML baseline retraining run...");
        try {
            // In production: Export behavior_history to S3 and trigger SageMaker training job
            log.info("[SCHEDULER] Baseline retraining stub executed successfully. " +
                    "Connect to SageMaker or local ML pipeline for full implementation.");
        } catch (Exception ex) {
            log.error("[SCHEDULER] Baseline retraining failed: {}", ex.getMessage(), ex);
        }
    }

    public void triggerManually() {
        log.info("[SCHEDULER] Manual baseline retraining triggered by administrator");
        retrainBaselines();
    }
}
