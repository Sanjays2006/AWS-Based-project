package com.cloudidentity.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Strongly-typed binding for all 'jwt.*' properties.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {

    /** Base64-encoded HMAC-SHA256 secret key */
    private String secret;

    /** Token validity in milliseconds (default: 1 hour) */
    private long expirationMs = 3_600_000L;

    /** Refresh token validity in milliseconds (default: 7 days) */
    private long refreshExpirationMs = 604_800_000L;
}
