package com.cloudidentity.service;

import com.cloudidentity.dto.response.PagedResponse;
import com.cloudidentity.dto.response.SessionResponse;
import com.cloudidentity.entity.Device;
import com.cloudidentity.entity.LoginLocation;
import com.cloudidentity.entity.User;
import com.cloudidentity.entity.UserSession;
import com.cloudidentity.repository.DeviceRepository;
import com.cloudidentity.repository.LoginLocationRepository;
import com.cloudidentity.repository.UserSessionRepository;
import com.cloudidentity.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserSessionRepository userSessionRepository;
    private final DeviceRepository deviceRepository;
    private final LoginLocationRepository loginLocationRepository;
    private final MapperUtil mapperUtil;

    @Transactional
    public UserSession createSession(User user, String token) {
        String ip = RequestContextUtil.getClientIp();
        String ua = RequestContextUtil.getUserAgent();

        // 1. Resolve or create Device
        String fingerprint = DeviceFingerprintUtil.generateFingerprint(ua, user.getEmail());
        Device device = deviceRepository.findByUser_IdAndDeviceFingerprint(user.getId(), fingerprint)
                .orElseGet(() -> {
                    Device dev = Device.builder()
                            .user(user)
                            .deviceFingerprint(fingerprint)
                            .browser(DeviceFingerprintUtil.parseBrowser(ua))
                            .operatingSystem(DeviceFingerprintUtil.parseOS(ua))
                            .deviceType(DeviceFingerprintUtil.parseDeviceType(ua))
                            .isTrusted(false)
                            .firstSeenAt(LocalDateTime.now())
                            .lastSeenAt(LocalDateTime.now())
                            .build();
                    return deviceRepository.save(dev);
                });

        device.setLastSeenAt(LocalDateTime.now());
        deviceRepository.save(device);

        // 2. Resolve login location
        GeoIpUtil.GeoLocation geo = GeoIpUtil.resolveIp(ip);

        // 3. Create Session record
        UserSession session = UserSession.builder()
                .user(user)
                .sessionToken(token)
                .device(device)
                .ipAddress(ip)
                .loginAt(LocalDateTime.now())
                .lastActivityAt(LocalDateTime.now())
                .isActive(true)
                .build();
        userSessionRepository.save(session);

        // 4. Save Login Location linked to Session
        LoginLocation location = LoginLocation.builder()
                .user(user)
                .session(session)
                .ipAddress(ip)
                .countryCode(geo.getCountryCode())
                .countryName(geo.getCountryName())
                .city(geo.getCity())
                .latitude(geo.getLatitude())
                .longitude(geo.getLongitude())
                .isVpn(geo.isVpn())
                .isTor(geo.isTor())
                .loginAt(LocalDateTime.now())
                .build();
        loginLocationRepository.save(location);

        log.info("Created session for user: {} from IP: {} (Location: {}, {})", 
                user.getUsername(), ip, geo.getCity(), geo.getCountryName());
        
        return session;
    }

    @Transactional
    public void invalidateSession(String token) {
        Optional<UserSession> sessionOpt = userSessionRepository.findBySessionToken(token);
        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            if (Boolean.TRUE.equals(session.getIsActive())) {
                session.setIsActive(false);
                session.setLogoutAt(LocalDateTime.now());
                
                long duration = Duration.between(session.getLoginAt(), session.getLogoutAt()).toMinutes();
                session.setDurationMinutes((int) duration);
                
                userSessionRepository.save(session);
                log.info("Session token invalidated successfully. Duration: {} minutes", duration);
            }
        }
    }

    @Transactional
    public void terminateSession(Long sessionId) {
        UserSession session = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        
        if (Boolean.TRUE.equals(session.getIsActive())) {
            session.setIsActive(false);
            session.setLogoutAt(LocalDateTime.now());
            long duration = Duration.between(session.getLoginAt(), session.getLogoutAt()).toMinutes();
            session.setDurationMinutes((int) duration);
            userSessionRepository.save(session);
        }
    }

    @Transactional
    public void invalidateAllSessionsOfUser(Long userId) {
        List<UserSession> activeSessions = userSessionRepository.findByUser_IdAndIsActiveTrue(userId);
        LocalDateTime now = LocalDateTime.now();
        for (UserSession s : activeSessions) {
            s.setIsActive(false);
            s.setLogoutAt(now);
            long duration = Duration.between(s.getLoginAt(), now).toMinutes();
            s.setDurationMinutes((int) duration);
        }
        userSessionRepository.saveAll(activeSessions);
        log.info("Invalidated all active sessions for user ID: {}", userId);
    }

    @Transactional(readOnly = true)
    public PagedResponse<SessionResponse> listUserSessions(Long userId, int page, int size) {
        Pageable pageable = PaginationUtil.createPageable(page, size, "loginAt", "DESC");
        Page<UserSession> sessionPage = userSessionRepository.findByUserId(userId, pageable);
        
        return PaginationUtil.toPagedResponse(sessionPage, 
                sessionPage.getContent().stream().map(mapperUtil::toSessionResponse).collect(Collectors.toList()));
    }
}
