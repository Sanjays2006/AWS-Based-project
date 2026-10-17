package com.cloudidentity.service;

import com.cloudidentity.dto.request.ChangePasswordRequest;
import com.cloudidentity.dto.request.LoginRequest;
import com.cloudidentity.dto.request.RegisterRequest;
import com.cloudidentity.dto.response.AuthResponse;
import com.cloudidentity.entity.Role;
import com.cloudidentity.entity.User;
import com.cloudidentity.exception.BadRequestException;
import com.cloudidentity.exception.DuplicateResourceException;
import com.cloudidentity.exception.ResourceNotFoundException;
import com.cloudidentity.repository.RoleRepository;
import com.cloudidentity.repository.UserRepository;
import com.cloudidentity.security.JwtTokenProvider;
import com.cloudidentity.security.SecurityConstants;
import com.cloudidentity.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final SessionService sessionService;
    private final AuditLogService auditLogService;

    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        log.info("Attempting login for user: {}", loginRequest.getUsername());

        // Find user by username or email first for lockout & brute force checks
        User user = userRepository.findByUsernameOrEmail(loginRequest.getUsername(), loginRequest.getUsername())
                .orElse(null);

        if (user != null) {
            // Check lockout state
            if (Boolean.FALSE.equals(user.getAccountNonLocked())) {
                if (user.getLockoutTime() != null && user.getLockoutTime().plusMinutes(15).isBefore(LocalDateTime.now())) {
                    // Lockout duration expired — unlock account
                    user.setAccountNonLocked(true);
                    user.setFailedAttemptCount(0);
                    user.setLockoutTime(null);
                    userRepository.save(user);
                    log.info("Lockout period expired for user '{}'. Account automatically unlocked.", user.getUsername());
                } else {
                    long minutesRemaining = 15;
                    if (user.getLockoutTime() != null) {
                        minutesRemaining = java.time.Duration.between(LocalDateTime.now(), user.getLockoutTime().plusMinutes(15)).toMinutes();
                        if (minutesRemaining < 1) minutesRemaining = 1;
                    }
                    log.warn("Login attempt rejected for locked account: {}", user.getUsername());
                    throw new BadRequestException("Account is locked due to 5 consecutive failed login attempts. Please try again in " + minutesRemaining + " minute(s).");
                }
            }
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );
        } catch (org.springframework.security.core.AuthenticationException ex) {
            if (user != null) {
                int attempts = (user.getFailedAttemptCount() == null ? 0 : user.getFailedAttemptCount()) + 1;
                user.setFailedAttemptCount(attempts);
                if (attempts >= 5) {
                    user.setAccountNonLocked(false);
                    user.setLockoutTime(LocalDateTime.now());
                    userRepository.save(user);
                    auditLogService.logAction(user, "ACCOUNT_LOCKED", "User", user.getId(),
                            "Account locked after 5 consecutive failed login attempts", null, null);
                    log.warn("Account locked after 5 failed login attempts for user: {}", user.getUsername());
                    throw new BadRequestException("Account locked after 5 consecutive failed login attempts. Please try again in 15 minutes.");
                } else {
                    userRepository.save(user);
                    int remaining = 5 - attempts;
                    throw new BadRequestException("Invalid username or password. Remaining attempts before account lockout: " + remaining);
                }
            }
            throw new BadRequestException("Invalid username or password.");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

        if (user == null) {
            user = userRepository.findById(principal.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        }

        // Successful authentication — reset failed login attempts
        user.setFailedAttemptCount(0);
        user.setAccountNonLocked(true);
        user.setLockoutTime(null);

        // Check MFA
        if (Boolean.TRUE.equals(user.getMfaEnabled())) {
            if (loginRequest.getMfaCode() == null || loginRequest.getMfaCode().isEmpty()) {
                log.info("MFA code required for user: {}", user.getUsername());
                return AuthResponse.builder()
                        .mfaRequired(true)
                        .username(user.getUsername())
                        .build();
            }
            
            // Simple validation: check if code matches "123456" or custom logic
            if (!validateMfaCode(user.getMfaSecret(), loginRequest.getMfaCode())) {
                log.warn("Invalid MFA code entered for user: {}", user.getUsername());
                throw new BadRequestException("Invalid Multi-Factor Authentication code");
            }
        }

        // Generate token
        String jwt = tokenProvider.generateToken(authentication);
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        // Update login stats
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Record tracking session
        sessionService.createSession(user, jwt);

        auditLogService.logAction(user, "LOGIN", "User", user.getId(), 
                "Successfully logged into the system", null, null);

        return AuthResponse.builder()
                .token(jwt)
                .tokenType(SecurityConstants.TOKEN_PREFIX.trim())
                .expiresInMs(tokenProvider.getExpirationMs())
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .roles(roles)
                .mfaRequired(false)
                .build();
    }

    @Transactional
    public void register(RegisterRequest registerRequest) {
        log.info("Registering user: {}", registerRequest.getUsername());

        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new DuplicateResourceException("Username '" + registerRequest.getUsername() + "' is already taken");
        }

        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new DuplicateResourceException("Email '" + registerRequest.getEmail() + "' is already registered");
        }

        Role viewerRole = roleRepository.findByName(SecurityConstants.ROLE_VIEWER)
                .orElseThrow(() -> new IllegalStateException("Default role viewer not found. Ensure DB is seeded."));

        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .fullName(registerRequest.getFullName())
                .isActive(true)
                .mfaEnabled(false)
                .roles(Set.of(viewerRole))
                .build();

        userRepository.save(user);

        auditLogService.logAction(user, "REGISTER", "User", user.getId(), 
                "Registered new user account", null, null);
    }

    @Transactional
    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith(SecurityConstants.TOKEN_PREFIX)) {
            return;
        }

        String token = authHeader.substring(SecurityConstants.TOKEN_PREFIX.length());
        sessionService.invalidateSession(token);
        
        log.info("Invalidated session token successfully");
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password does not match standard records");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditLogService.logAction(user, "CHANGE_PASSWORD", "User", user.getId(), 
                "Updated user login password", null, null);
    }

    @Transactional
    public String generateMfaSecret(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Mock simple secret generation
        String secret = "SECRET" + userId + "KEY";
        user.setMfaSecret(secret);
        userRepository.save(user);
        
        return secret;
    }

    @Transactional
    public void verifyAndEnableMfa(Long userId, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!validateMfaCode(user.getMfaSecret(), code)) {
            throw new BadRequestException("Invalid verification code");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);

        auditLogService.logAction(user, "ENABLE_MFA", "User", user.getId(), 
                "Enabled Multi-Factor Authentication (TOTP)", null, null);
    }

    private boolean validateMfaCode(String secret, String code) {
        // Simulating MFA TOTP validator (in production, use a library like GoogleAuth)
        // Accepts code "123456" for demo flow or matches simple hashing
        return "123456".equals(code) || (secret != null && String.valueOf(Math.abs(secret.hashCode()) % 1000000).equals(code));
    }
}
