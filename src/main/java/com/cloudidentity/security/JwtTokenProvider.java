package com.cloudidentity.security;

import com.cloudidentity.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;

/**
 * Handles JWT token generation, validation, and claims extraction.
 * Uses HMAC-SHA256 (HS256) algorithm.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    /**
     * Generates a signed JWT token for an authenticated principal.
     *
     * @param authentication the Spring Security Authentication object
     * @return signed JWT string
     */
    public String generateToken(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getExpirationMs());

        return Jwts.builder()
                .subject(principal.getUsername())
                .claim("userId", principal.getId())
                .claim("email", principal.getEmail())
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Generates a JWT token directly from a username (used in service layer).
     */
    public String generateTokenFromUsername(String username, Long userId, String email, List<String> roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getExpirationMs());

        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .claim("email", email)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extracts the username (subject) from a valid JWT token.
     */
    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Extracts the userId claim from a valid JWT token.
     */
    public Long getUserIdFromToken(String token) {
        Object userId = parseClaims(token).get("userId");
        if (userId instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    /**
     * Validates a JWT token — checks signature and expiration.
     *
     * @param token the JWT string
     * @return true if valid
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (SignatureException ex) {
            log.warn("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("Malformed JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.warn("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Returns the token expiration duration in milliseconds.
     */
    public long getExpirationMs() {
        return jwtConfig.getExpirationMs();
    }

    // ===== Private helpers =====

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(jwtConfig.getSecret());
        } catch (Exception e) {
            keyBytes = jwtConfig.getSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
