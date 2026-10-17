package com.cloudidentity.security;

/**
 * Application-wide security constants.
 */
public final class SecurityConstants {

    private SecurityConstants() {}

    public static final String TOKEN_PREFIX           = "Bearer ";
    public static final String AUTHORIZATION_HEADER   = "Authorization";
    public static final String CONTENT_TYPE_HEADER    = "Content-Type";

    public static final String ROLE_ADMIN             = "ADMIN";
    public static final String ROLE_ANALYST           = "ANALYST";
    public static final String ROLE_VIEWER            = "VIEWER";

    public static final String ROLE_PREFIX            = "ROLE_";

    /** Paths that are publicly accessible without a JWT */
    public static final String[] PUBLIC_PATHS = {
        "/api/v1/auth/login",
        "/api/v1/auth/register",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/v3/api-docs/**",
        "/actuator/health",
        "/actuator/info",
        "/api/v1/diagnostics/**"
    };

    /** Static web assets served by Spring Boot */
    public static final String[] STATIC_PATHS = {
        "/",
        "/index.html",
        "/pages/**",
        "/css/**",
        "/js/**",
        "/images/**",
        "/favicon.ico"
    };
}
