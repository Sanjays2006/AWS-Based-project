package com.cloudidentity.scheduler;

import com.cloudidentity.config.AppProperties;
import com.cloudidentity.entity.ActivityLog;
import com.cloudidentity.entity.IamIdentity;
import com.cloudidentity.repository.ActivityLogRepository;
import com.cloudidentity.repository.IamIdentityRepository;
import com.cloudidentity.service.ThreatDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.cloudtrail.model.Event;
import software.amazon.awssdk.services.cloudtrail.model.LookupAttribute;
import software.amazon.awssdk.services.cloudtrail.model.LookupAttributeKey;
import software.amazon.awssdk.services.cloudtrail.model.LookupEventsRequest;
import software.amazon.awssdk.services.cloudtrail.model.LookupEventsResponse;
import software.amazon.awssdk.services.cloudtrail.model.Resource;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Polls AWS CloudTrail every 5 minutes (configurable) via the LookupEvents API,
 * ingests new management events into {@code activity_logs}, and runs each one
 * through the {@link ThreatDetectionService} evaluation pipeline.
 *
 * <p>Enabled via {@code app.scheduler.cloudtrail-enabled=true}
 * (env var {@code SCHEDULER_CLOUDTRAIL_ENABLED=true}) and requires AWS
 * credentials with the {@code cloudtrail:LookupEvents} permission.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloudTrailIngestionScheduler {

    private final AppProperties appProperties;
    private final CloudTrailClient cloudTrailClient;
    private final ActivityLogRepository activityLogRepository;
    private final IamIdentityRepository iamIdentityRepository;
    private final ThreatDetectionService threatDetectionService;

    /** How far back to look on each poll (overlaps slightly to avoid gaps). */
    private static final long LOOKBACK_MINUTES = 10;
    /** Safety cap on events processed per run. */
    private static final int MAX_EVENTS_PER_RUN = 200;

    @Scheduled(fixedDelayString = "${app.scheduler.cloudtrail-poll-ms:300000}")
    public void pollAndIngestCloudTrailEvents() {
        if (!appProperties.getScheduler().isCloudtrailEnabled()) {
            log.debug("CloudTrail ingestion scheduler is disabled. Skipping.");
            return;
        }

        log.info("[SCHEDULER] Starting CloudTrail event ingestion run...");
        try {
            Instant end = Instant.now();
            Instant start = end.minus(LOOKBACK_MINUTES, ChronoUnit.MINUTES);

            int ingested = 0;
            int evaluated = 0;
            String nextToken = null;

            do {
                LookupEventsRequest.Builder reqBuilder = LookupEventsRequest.builder()
                        .startTime(start)
                        .endTime(end)
                        // Focus on IAM-user activity; remove this attribute to ingest all events.
                        .lookupAttributes(LookupAttribute.builder()
                                .attributeKey(LookupAttributeKey.EVENT_SOURCE)
                                .attributeValue("iam.amazonaws.com")
                                .build());
                if (nextToken != null) {
                    reqBuilder.nextToken(nextToken);
                }

                LookupEventsResponse response = cloudTrailClient.lookupEvents(reqBuilder.build());

                for (Event event : response.events()) {
                    if (ingested >= MAX_EVENTS_PER_RUN) {
                        break;
                    }
                    ActivityLog saved = ingestEvent(event);
                    if (saved != null) {
                        ingested++;
                        try {
                            threatDetectionService.evaluateActivityLog(saved);
                            evaluated++;
                        } catch (Exception evalEx) {
                            log.warn("[SCHEDULER] Threat evaluation failed for event {}: {}",
                                    event.eventId(), evalEx.getMessage());
                        }
                    }
                }

                nextToken = response.nextToken();
            } while (nextToken != null && ingested < MAX_EVENTS_PER_RUN);

            log.info("[SCHEDULER] CloudTrail ingestion completed. Ingested={} Evaluated={}", ingested, evaluated);
        } catch (Exception ex) {
            log.error("[SCHEDULER] CloudTrail ingestion failed: {}", ex.getMessage(), ex);
        }
    }

    /**
     * Maps a CloudTrail {@link Event} to an {@link ActivityLog} and persists it.
     * Returns {@code null} when the event is a duplicate or cannot be mapped.
     */
    private ActivityLog ingestEvent(Event event) {
        try {
            IamIdentity identity = resolveIdentity(event);

            LocalDateTime eventTime = event.eventTime() != null
                    ? LocalDateTime.ofInstant(event.eventTime(), ZoneOffset.UTC)
                    : LocalDateTime.now();

            ActivityLog activityLog = ActivityLog.builder()
                    .iamIdentity(identity)
                    .eventName(event.eventName())
                    .eventSource(event.eventSource())
                    .eventTime(eventTime)
                    .awsService(event.eventSource())
                    .dataVolumeBytes(0L)
                    .build();

            return activityLogRepository.save(activityLog);
        } catch (Exception ex) {
            log.warn("[SCHEDULER] Failed to ingest CloudTrail event {}: {}",
                    event.eventId(), ex.getMessage());
            return null;
        }
    }

    /**
     * Finds the IAM identity referenced by the event (by ARN), creating a
     * lightweight monitored record on first sighting. Returns {@code null}
     * when the event has no resolvable IAM resource.
     */
    private IamIdentity resolveIdentity(Event event) {
        String arn = null;
        String name = event.username();

        List<Resource> resources = event.resources();
        if (resources != null) {
            for (Resource r : resources) {
                if (r.resourceName() != null && r.resourceName().startsWith("arn:aws:iam:")) {
                    arn = r.resourceName();
                    break;
                }
            }
        }
        if (arn == null && name != null) {
            // Fall back to a synthetic ARN keyed by the username.
            arn = "arn:aws:iam::unknown:user/" + name;
        }
        if (arn == null) {
            return null;
        }

        final String resolvedArn = arn;
        final String resolvedName = (name != null) ? name : resolvedArn;
        return iamIdentityRepository.findByArn(resolvedArn)
                .orElseGet(() -> iamIdentityRepository.save(
                        IamIdentity.builder()
                                .arn(resolvedArn)
                                .identityName(resolvedName)
                                .identityType("USER")
                                .isMonitored(true)
                                .build()));
    }

    /** Manual trigger endpoint — callable from admin panel */
    public void triggerManually() {
        log.info("[SCHEDULER] Manual CloudTrail ingestion triggered by administrator");
        pollAndIngestCloudTrailEvents();
    }
}
