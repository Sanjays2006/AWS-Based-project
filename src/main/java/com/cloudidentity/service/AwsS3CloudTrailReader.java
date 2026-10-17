package com.cloudidentity.service;

import com.cloudidentity.dto.response.IngestionResultResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Reads CloudTrail log files from the configured S3 bucket and feeds the parsed
 * events through the shared {@link CloudTrailEventIngestor} (parse ->
 * de-duplicate -> persist -> threat-evaluate).
 *
 * <p>CloudTrail delivers logs as gzipped JSON objects under a key prefix. This
 * reader paginates through the bucket using S3 continuation tokens, downloads
 * each {@code *.json.gz} object, gunzips it, extracts the {@code Records} array,
 * and ingests each record.
 *
 * <p>S3 access is strictly read-only ({@code s3:ListBucket} + {@code s3:GetObject}).
 *
 * <p>Resilient: when the bucket is not configured or S3 is unreachable, methods
 * return a status report with {@code sourceAvailable=false} rather than throwing.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AwsS3CloudTrailReader {

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;
    private final CloudTrailEventIngestor cloudTrailEventIngestor;

    @Value("${aws.cloudtrail.s3-bucket:}")
    private String bucket;

    @Value("${aws.cloudtrail.s3-prefix:AWSLogs/}")
    private String prefix;

    /** Max S3 objects to scan per invocation (across all pages). */
    @Value("${aws.cloudtrail.s3-max-objects:200}")
    private int maxObjects;

    /** S3 ListObjectsV2 page size. */
    private static final int PAGE_SIZE = 100;

    /**
     * Lists, parses, and ingests recent CloudTrail log objects from S3,
     * reusing the shared ingestion pipeline.
     *
     * @return a status report describing what was scanned, ingested, skipped,
     *         evaluated, and failed.
     */
    public IngestionResultResponse ingestRecentEvents() {
        IngestionResultResponse result = IngestionResultResponse.builder()
                .source("S3")
                .build();

        if (bucket == null || bucket.isBlank()) {
            result.setSourceAvailable(false);
            result.setMessage("CloudTrail S3 bucket is not configured (aws.cloudtrail.s3-bucket).");
            log.debug("[AWS S3] CloudTrail bucket not configured — skipping S3 ingestion.");
            return result;
        }

        log.info("[AWS S3] Starting CloudTrail S3 ingestion from s3://{}/{}", bucket, prefix);
        try {
            String continuationToken = null;
            int objectsRead = 0;

            outer:
            do {
                ListObjectsV2Request.Builder listReq = ListObjectsV2Request.builder()
                        .bucket(bucket)
                        .prefix(prefix)
                        .maxKeys(PAGE_SIZE);
                if (continuationToken != null) {
                    listReq.continuationToken(continuationToken);
                }

                ListObjectsV2Response listing = s3Client.listObjectsV2(listReq.build());

                for (S3Object obj : listing.contents()) {
                    if (!isLogObject(obj.key())) {
                        continue;
                    }
                    if (objectsRead >= maxObjects) {
                        log.info("[AWS S3] Reached max object cap ({}). Stopping.", maxObjects);
                        break outer;
                    }
                    objectsRead++;
                    ingestObject(obj.key(), result);
                }

                continuationToken = Boolean.TRUE.equals(listing.isTruncated())
                        ? listing.nextContinuationToken() : null;
            } while (continuationToken != null && objectsRead < maxObjects);

            result.setObjectsRead(objectsRead);
            result.setMessage(String.format(
                    "S3 ingestion complete: %d objects read, %d scanned, %d ingested, %d duplicates, %d evaluated, %d failed.",
                    objectsRead, result.getScanned(), result.getIngested(),
                    result.getDuplicates(), result.getEvaluated(), result.getFailed()));
            log.info("[AWS S3] {}", result.getMessage());
        } catch (Exception ex) {
            result.setSourceAvailable(false);
            result.setMessage("S3 ingestion failed: " + ex.getMessage());
            result.addError(ex.getMessage());
            log.warn("[AWS S3] Failed to ingest CloudTrail logs from s3://{}/{}: {}",
                    bucket, prefix, ex.getMessage());
        }
        return result;
    }

    private boolean isLogObject(String key) {
        return key != null && (key.endsWith(".json.gz") || key.endsWith(".gz"));
    }

    /** Downloads one gzipped CloudTrail object, extracts Records, and ingests each. */
    private void ingestObject(String key, IngestionResultResponse result) {
        try (ResponseInputStream<GetObjectResponse> s3is = s3Client.getObject(
                GetObjectRequest.builder().bucket(bucket).key(key).build())) {

            byte[] decompressed = gunzip(s3is);
            JsonNode root = objectMapper.readTree(decompressed);
            JsonNode recordsNode = root.get("Records");
            if (recordsNode != null && recordsNode.isArray()) {
                for (JsonNode record : recordsNode) {
                    cloudTrailEventIngestor.ingestRecord(record, result);
                }
            }
        } catch (Exception ex) {
            result.addError("Failed to parse object '" + key + "': " + ex.getMessage());
            log.warn("[AWS S3] Failed to parse object '{}': {}", key, ex.getMessage());
        }
    }

    private byte[] gunzip(InputStream compressed) throws Exception {
        try (GZIPInputStream gis = new GZIPInputStream(compressed);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = gis.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            return out.toByteArray();
        }
    }
}
