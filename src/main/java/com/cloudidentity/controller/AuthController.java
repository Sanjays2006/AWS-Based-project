package com.cloudidentity.controller;

import com.cloudidentity.dto.request.ChangePasswordRequest;
import com.cloudidentity.dto.request.LoginRequest;
import com.cloudidentity.dto.request.MfaEnrollRequest;
import com.cloudidentity.dto.request.RegisterRequest;
import com.cloudidentity.dto.response.AuthResponse;
import com.cloudidentity.dto.response.UserResponse;
import com.cloudidentity.entity.User;
import com.cloudidentity.repository.UserRepository;
import com.cloudidentity.security.UserPrincipal;
import com.cloudidentity.service.AuthService;
import com.cloudidentity.util.MapperUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final MapperUtil mapperUtil;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        authService.register(registerRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Registration successful! Please login."
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        authService.logout(authHeader);
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        return ResponseEntity.ok(mapperUtil.toUserResponse(user));
    }

    @PutMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(@AuthenticationPrincipal UserPrincipal principal,
                                                              @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }

    @PostMapping("/mfa/enroll")
    public ResponseEntity<Map<String, String>> enrollMfa(@AuthenticationPrincipal UserPrincipal principal) {
        String secret = authService.generateMfaSecret(principal.getId());
        return ResponseEntity.ok(Map.of(
                "secret", secret,
                "qrCodeUrl", "https://api.qrserver.com/v1/create-qr-code/?size=200x200&data=otpauth://totp/CloudIdentity:" 
                        + principal.getUsername() + "?secret=" + secret + "&issuer=CloudIdentity"
        ));
    }

    @PostMapping("/mfa/verify")
    public ResponseEntity<Map<String, String>> verifyMfa(@AuthenticationPrincipal UserPrincipal principal,
                                                         @Valid @RequestBody MfaEnrollRequest request) {
        authService.verifyAndEnableMfa(principal.getId(), request.getCode());
        return ResponseEntity.ok(Map.of("message", "MFA has been successfully configured and enabled"));
    }
}
