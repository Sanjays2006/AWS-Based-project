package com.cloudidentity.security;

import com.cloudidentity.config.JwtConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtTokenProviderTest {

    @Mock
    private JwtConfig jwtConfig;

    @InjectMocks
    private JwtTokenProvider jwtTokenProvider;

    private String base64Secret;

    @BeforeEach
    void setUp() {
        // 256-bit key encoded in Base64
        byte[] secretBytes = "01234567890123456789012345678901".getBytes();
        base64Secret = Base64.getEncoder().encodeToString(secretBytes);
    }

    @Test
    @DisplayName("Should generate valid JWT token and parse username and claims")
    void testGenerateAndValidateToken() {
        when(jwtConfig.getSecret()).thenReturn(base64Secret);
        when(jwtConfig.getExpirationMs()).thenReturn(3600000L);

        String token = jwtTokenProvider.generateTokenFromUsername("testadmin", 42L, "admin@cloudidentity.io", List.of("ROLE_ADMIN"));

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("testadmin", jwtTokenProvider.getUsernameFromToken(token));
        assertEquals(42L, jwtTokenProvider.getUserIdFromToken(token));
    }

    @Test
    @DisplayName("Should return false when validating an invalid JWT token")
    void testValidateToken_InvalidToken() {
        when(jwtConfig.getSecret()).thenReturn(base64Secret);

        assertFalse(jwtTokenProvider.validateToken("invalid.jwt.token"));
    }
}
