package com.cloudidentity.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/diagnostics")
@RequiredArgsConstructor
public class SystemDiagnosticsController {

    private final JdbcTemplate jdbcTemplate;
    private final StsClient stsClient;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getDevDiagnostics() {
        Map<String, Object> diagnostics = new LinkedHashMap<>();

        boolean dbConnected = false;
        String postgresVersion = "UNKNOWN";
        try {
            String dbResult = jdbcTemplate.queryForObject("SELECT version()", String.class);
            if (dbResult != null) {
                dbConnected = true;
                postgresVersion = dbResult.contains("PostgreSQL") ? dbResult.split(",")[0] : dbResult;
            }
        } catch (Exception e) {
            log.error("Diagnostics DB check failed", e);
        }

        // Live AWS connectivity probe via STS GetCallerIdentity.
        boolean awsConnected = false;
        String awsAccount = null;
        String awsStatus = "NOT_CONFIGURED";
        try {
            GetCallerIdentityResponse id = stsClient.getCallerIdentity();
            awsConnected = true;
            awsAccount = id.account();
            awsStatus = "CONNECTED (account " + awsAccount + ", region " + awsRegion + ")";
        } catch (Exception e) {
            awsStatus = "DISCONNECTED (" + e.getMessage() + ")";
            log.debug("Diagnostics AWS check failed: {}", e.getMessage());
        }

        diagnostics.put("profile", activeProfile);
        diagnostics.put("devMode", "dev".equalsIgnoreCase(activeProfile) || "local".equalsIgnoreCase(activeProfile));
        diagnostics.put("backendStatus", "UP");
        diagnostics.put("databaseConnected", dbConnected);
        diagnostics.put("jwtStatus", "ACTIVE");
        diagnostics.put("springBootStatus", "RUNNING (v3.2.5)");
        diagnostics.put("postgresStatus", dbConnected ? "CONNECTED (" + postgresVersion + ")" : "DISCONNECTED");
        diagnostics.put("awsConnected", awsConnected);
        diagnostics.put("awsAccount", awsAccount);
        diagnostics.put("awsStatus", awsStatus);
        diagnostics.put("cloudTrailStatus", awsConnected ? "INGESTION_READY" : "INGESTION_STANDBY");
        diagnostics.put("version", "1.0.0-ENTERPRISE");

        return ResponseEntity.ok(diagnostics);
    }
}
