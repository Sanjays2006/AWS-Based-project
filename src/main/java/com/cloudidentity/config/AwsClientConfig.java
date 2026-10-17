package com.cloudidentity.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.guardduty.GuardDutyClient;
import software.amazon.awssdk.services.iam.IamClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.model.GetCallerIdentityResponse;

/**
 * Central AWS SDK v2 client configuration.
 *
 * <p>Credential resolution strategy:
 * <ul>
 *   <li>If {@code aws.access-key-id} / {@code aws.secret-access-key} are provided
 *       (via env vars or application.yml), use static credentials.</li>
 *   <li>Otherwise fall back to the {@link DefaultCredentialsProvider} chain
 *       (env vars, system properties, ~/.aws/credentials profile, or the
 *       IAM instance/container role when running on AWS).</li>
 * </ul>
 *
 * <p>Building a client does NOT contact AWS — the first real network call happens
 * only when a service method is invoked, so these beans are safe to create even
 * when no credentials are present. The {@link #awsConnectionVerifier} runner
 * performs a single best-effort STS check at startup and never fails the boot.
 */
@Slf4j
@Configuration
public class AwsClientConfig {

    @Value("${aws.region:us-east-1}")
    private String region;

    @Value("${aws.access-key-id:}")
    private String accessKeyId;

    @Value("${aws.secret-access-key:}")
    private String secretAccessKey;

    /**
     * Returns static credentials when explicit keys are configured, otherwise
     * the default AWS credential provider chain.
     */
    private AwsCredentialsProvider credentialsProvider() {
        if (accessKeyId != null && !accessKeyId.isBlank()
                && secretAccessKey != null && !secretAccessKey.isBlank()) {
            log.info("[AWS] Using static credentials (access key id ending '...{}')",
                    accessKeyId.length() >= 4 ? accessKeyId.substring(accessKeyId.length() - 4) : "****");
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId, secretAccessKey));
        }
        log.info("[AWS] No static credentials configured — using DefaultCredentialsProvider chain "
                + "(env vars / system properties / profile / IAM role).");
        return DefaultCredentialsProvider.create();
    }

    private Region resolvedRegion() {
        return Region.of(region);
    }

    // ===== Regional service clients =====

    @Bean
    public StsClient stsClient() {
        return StsClient.builder()
                .region(resolvedRegion())
                .credentialsProvider(credentialsProvider())
                .build();
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(resolvedRegion())
                .credentialsProvider(credentialsProvider())
                .build();
    }

    @Bean
    public CloudTrailClient cloudTrailClient() {
        return CloudTrailClient.builder()
                .region(resolvedRegion())
                .credentialsProvider(credentialsProvider())
                .build();
    }

    @Bean
    public SnsClient snsClient() {
        return SnsClient.builder()
                .region(resolvedRegion())
                .credentialsProvider(credentialsProvider())
                .build();
    }

    @Bean
    public CloudWatchClient cloudWatchClient() {
        return CloudWatchClient.builder()
                .region(resolvedRegion())
                .credentialsProvider(credentialsProvider())
                .build();
    }

    @Bean
    public GuardDutyClient guardDutyClient() {
        return GuardDutyClient.builder()
                .region(resolvedRegion())
                .credentialsProvider(credentialsProvider())
                .build();
    }

    /**
     * IAM is a global service — its endpoint lives in {@code AWS_GLOBAL}.
     */
    @Bean
    public IamClient iamClient() {
        return IamClient.builder()
                .region(Region.AWS_GLOBAL)
                .credentialsProvider(credentialsProvider())
                .build();
    }

    // ===== Startup connectivity check =====

    /**
     * Best-effort STS GetCallerIdentity probe at startup. Logs the resolved
     * account / ARN when credentials work; logs a clear warning (but does NOT
     * crash the application) when they are missing or invalid.
     */
    @Bean
    public CommandLineRunner awsConnectionVerifier(StsClient stsClient) {
        return args -> {
            try {
                GetCallerIdentityResponse id = stsClient.getCallerIdentity();
                log.info("[AWS] Connection verified OK  account={} arn={} (region={})",
                        id.account(), id.arn(), region);
            } catch (Exception ex) {
                log.warn("[AWS] Connection NOT established — AWS features will be inactive. "
                        + "Reason: {}. Set AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY / AWS_REGION "
                        + "(or an IAM role) to enable live AWS integration.", ex.getMessage());
            }
        };
    }
}
