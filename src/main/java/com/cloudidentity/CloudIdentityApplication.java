package com.cloudidentity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Cloud Identity Behavior Fingerprinting System
 *
 * <p>Entry point for the cloud security platform that monitors AWS IAM identities,
 * learns normal behavior patterns, detects anomalies, computes risk scores,
 * and provides automated security responses.
 *
 * @version 1.0.0
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties
public class CloudIdentityApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudIdentityApplication.class, args);
    }
}
