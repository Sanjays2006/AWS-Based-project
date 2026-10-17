package com.cloudidentity.config;

import com.cloudidentity.security.JwtAuthenticationFilter;
import com.cloudidentity.security.SecurityConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.cloudidentity.security.CustomUserDetailsService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Spring Security configuration.
 * Stateless JWT-based authentication with role-based access control.
 */
@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(new CorsConfig(appProperties).corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Static frontend resources
                        .requestMatchers(SecurityConstants.STATIC_PATHS).permitAll()
                        // Public API endpoints
                        .requestMatchers(SecurityConstants.PUBLIC_PATHS).permitAll()
                        // Admin-only endpoints
                        .requestMatchers("/api/v1/users/**").hasRole(SecurityConstants.ROLE_ADMIN)
                        .requestMatchers("/api/v1/settings/**").hasRole(SecurityConstants.ROLE_ADMIN)
                        .requestMatchers("/api/v1/scheduler/**").hasRole(SecurityConstants.ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/risk-rules/**").hasRole(SecurityConstants.ROLE_ADMIN)
                        // Analyst + Admin endpoints
                        .requestMatchers("/api/v1/investigation/**").hasAnyRole(
                                SecurityConstants.ROLE_ADMIN, SecurityConstants.ROLE_ANALYST)
                        .requestMatchers(HttpMethod.POST, "/api/v1/responses/**").hasAnyRole(
                                SecurityConstants.ROLE_ADMIN, SecurityConstants.ROLE_ANALYST)
                        // All other API endpoints require any authenticated role
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Returns 401 Unauthorized for unauthenticated requests to protected endpoints.
     */
    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) -> {
            log.warn("Unauthorized request to: {} — {}", request.getRequestURI(), ex.getMessage());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            objectMapper.writeValue(response.getOutputStream(), Map.of(
                    "timestamp", LocalDateTime.now().toString(),
                    "status", HttpStatus.UNAUTHORIZED.value(),
                    "error", "Unauthorized",
                    "message", "Authentication required. Please provide a valid JWT token.",
                    "path", request.getRequestURI()
            ));
        };
    }

    /**
     * Returns 403 Forbidden when an authenticated user lacks the required role.
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest request, HttpServletResponse response,
                org.springframework.security.access.AccessDeniedException ex) -> {
            log.warn("Access denied to: {} — {}", request.getRequestURI(), ex.getMessage());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            try {
                objectMapper.writeValue(response.getOutputStream(), Map.of(
                        "timestamp", LocalDateTime.now().toString(),
                        "status", HttpStatus.FORBIDDEN.value(),
                        "error", "Forbidden",
                        "message", "You do not have permission to access this resource.",
                        "path", request.getRequestURI()
                ));
            } catch (IOException e) {
                log.error("Error writing access denied response", e);
            }
        };
    }
}
