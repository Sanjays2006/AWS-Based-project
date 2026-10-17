package com.cloudidentity.service;

import com.cloudidentity.dto.response.IngestionResultResponse;
import com.cloudidentity.entity.ActivityLog;
import com.cloudidentity.entity.IamIdentity;
import com.cloudidentity.repository.ActivityLogRepository;
import com.cloudidentity.repository.IamIdentityRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

/**
 * Shared ingestion logic for a single CloudTrail event record (parsed as a
 * Jackson {@link JsonNode}). Centralises the parse -> de-duplicate -> persist ->
 * threat-evaluate flow so both the S3 log-file reader and the LookupEvents API
 * scheduler reuse one implementation instead of duplicating it.
 *
 * <p>De-duplication is enforced by the CloudTrail {@code eventID}: an event
 * whose id already exists in {@code activity_logs} is skipped.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CloudTrailEventIngestor {

    private final ActivityLogRepository activityLogRepository;
    private final IamIdentityRepository iamIdentityRepository;
    private final ThreatDetectionService threatDetectionService;

    /**
     * Ingests one CloudTrail record, updating the supplied result tallies.
     * Each record is processed in its own transaction so a single bad record
     * cannot roll back a whole batch.
     *
     * @param record a CloudTrail event JSON object (an element of the "Records" array)
     * @param result mutable running tally for the ingestion run
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ingestRecord(JsonNode record, IngestionResultResponse result) {
        result.setScanned(result.getScanned() + 1);
        String eventId = text(record, "eventID");
        try {
            // 1. De-duplication by CloudTrail event id.
            if (eventId != null && activityLogRepository.existsByEventId(eventId)) {
                result.setDuplicates(result.getDuplicates() + 1);
                log.debug("[INGEST] Skipping duplicate CloudTrail event {}", eventId);
                return;
            }

            // 2. Resolve (or lazily create) the IAM identity for this event.
            IamIdentity identity = resolveIdentity(record);

            // 3. Map and persist the ActivityLog.
            ActivityLog activityLog = ActivityLog.builder()
                    .eventId(eventId)
                    .iamIdentity(identity)
                    .eventName(text(record, "eventName"))
                    .eventSource(text(record, "eventSource"))
                    .awsService(text(record, "eventSource"))
                    .eventTime(parseEventTime(text(record, "eventTime")))
                    .sourceIp(text(record, "sourceIPAddress"))
                    .userAgent(truncate(text(record, "userAgent"), 500))
                    .region(text(record, "awsRegion"))
                    .dataVolumeBytes(0L)
                    .build();

            ActivityLog saved = activityLogRepository.save(activityLog);
            result.setIngested(result.getIngested() + 1);

            // 4. Run through the existing threat-detection pipeline.
            try {
                threatDetectionService.evaluateActivityLog(saved);
                result.setEvaluated(result.getEvaluated() + 1);
            } catch (Exception evalEx) {
                result.setFailed(result.getFailed() + 1);
                result.addError("Threat evaluation failed for event " + eventId + ": " + evalEx.getMessage());
                log.warn("[INGEST] Threat evaluation failed for event {}: {}", eventId, evalEx.getMessage());
            }
        } catch (Exception ex) {
            result.setFailed(result.getFailed() + 1);
            result.addError("Failed to ingest event " + eventId + ": " + ex.getMessage());
            log.warn("[INGEST] Failed to ingest CloudTrail event {}: {}", eventId, ex.getMessage());
        }
    }

    /**
     * Resolves the IAM identity referenced by the event's {@code userIdentity}
     * block (by ARN), creating a lightweight monitored record on first sighting.
     * Returns {@code null} when no identity can be resolved.
     */
    private IamIdentity resolveIdentity(JsonNode record) {
        JsonNode userIdentity = record.get("userIdentity");
        String arn = userIdentity != null ? text(userIdentity, "arn") : null;
        String name = userIdentity != null ? text(userIdentity, "userName") : null;
        String type = userIdentity != null ? text(userIdentity, "type") : null;
        String accountId = userIdentity != null ? text(userIdentity, "accountId") : null;

        if (arn == null && name != null) {
            arn = "arn:aws:iam::" + (accountId != null ? accountId : "unknown") + ":user/" + name;
        }
        if (arn == null) {
            return null;
        }

        final String resolvedArn = arn;
        final String resolvedName = (name != null) ? name : resolvedArn;
        final String resolvedType = mapIdentityType(type);
        final String resolvedAccount = accountId;
        return iamIdentityRepository.findByArn(resolvedArn)
                .orElseGet(() -> iamIdentityRepository.save(IamIdentity.builder()
                        .arn(resolvedArn)
                        .identityName(resolvedName)
                        .identityType(resolvedType)
                        .accountId(resolvedAccount)
                        .isMonitored(true)
                        .build()));
    }

    /** Maps a CloudTrail userIdentity.type to the platform USER|ROLE|GROUP scheme. */
    private String mapIdentityType(String ctType) {
        if (ctType == null) {
            return "USER";
        }
        return switch (ctType) {
            case "AssumedRole", "Role" -> "ROLE";
            case "Group" -> "GROUP";
            default -> "USER";
        };
    }

    /** CloudTrail eventTime is ISO-8601 (e.g. 2026-10-17T12:34:56Z). */
    private LocalDateTime parseEventTime(String eventTime) {
        if (eventTime == null || eventTime.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return OffsetDateTime.parse(eventTime).atZoneSameInstant(ZoneOffset.UTC).toLocalDateTime();
        } catch (DateTimeParseException ex) {
            log.debug("[INGEST] Unparseable eventTime '{}', defaulting to now()", eventTime);
            return LocalDateTime.now();
        }
    }

    /** Null-safe JSON text extraction. */
    private String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode v = node.get(field);
        return (v != null && !v.isNull()) ? v.asText() : null;
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() > max ? s.substring(0, max) : s;
    }
}
