package com.cloudidentity.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Ingestion status report returned by CloudTrail ingestion operations
 * (both the S3 log-file path and the LookupEvents API path).
 */
@Data
@Builder
public class IngestionResultResponse {

    /** Logical source of the ingestion run, e.g. "S3" or "CLOUDTRAIL_API". */
    private String source;

    /** Total CloudTrail event records scanned from the source. */
    @Builder.Default
    private int scanned = 0;

    /** New events successfully persisted. */
    @Builder.Default
    private int ingested = 0;

    /** Events skipped because they were already ingested (duplicate event id). */
    @Builder.Default
    private int duplicates = 0;

    /** Persisted events that were run through the threat-detection pipeline. */
    @Builder.Default
    private int evaluated = 0;

    /** Events that failed to map/persist/evaluate. */
    @Builder.Default
    private int failed = 0;

    /** Number of S3 objects (log files) read; 0 for the API path. */
    @Builder.Default
    private int objectsRead = 0;

    /** Whether the ingestion source was configured and reachable. */
    @Builder.Default
    private boolean sourceAvailable = true;

    /** Human-readable summary / status message. */
    private String message;

    /** Bounded list of error messages encountered (capped to avoid huge payloads). */
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    private static final int MAX_ERRORS = 50;

    /** Records a non-fatal error, keeping the list bounded. */
    public void addError(String error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        if (errors.size() < MAX_ERRORS) {
            errors.add(error);
        }
    }
}
