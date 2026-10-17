package com.cloudidentity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String tokenType;
    private long expiresInMs;
    private Long userId;
    private String username;
    private String email;
    private List<String> roles;
    private boolean mfaRequired;
    private String mfaSecretQrCodeUrl; // For TOTP setup if registering/enrolling
}
