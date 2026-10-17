package com.cloudidentity.engine.impl;

import com.cloudidentity.engine.BehaviorEngine;
import com.cloudidentity.engine.model.BehaviorVector;
import com.cloudidentity.entity.ActivityLog;
import com.cloudidentity.entity.UserSession;
import com.cloudidentity.util.DeviceFingerprintUtil;
import com.cloudidentity.util.GeoIpUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DefaultBehaviorEngine implements BehaviorEngine {

    @Override
    public BehaviorVector extractVector(ActivityLog log) {
        if (log == null) return null;

        LocalDateTime time = log.getEventTime() != null ? log.getEventTime() : LocalDateTime.now();
        GeoIpUtil.GeoLocation geo = GeoIpUtil.resolveIp(log.getSourceIp());

        return BehaviorVector.builder()
                .identityArn(log.getIamIdentity() != null ? log.getIamIdentity().getArn() : "Unknown")
                .eventTime(time)
                .region(log.getRegion() != null ? log.getRegion() : "us-east-1")
                .countryCode(geo.getCountryCode())
                .sourceIp(log.getSourceIp())
                .userAgent(log.getUserAgent())
                .browser(DeviceFingerprintUtil.parseBrowser(log.getUserAgent()))
                .os(DeviceFingerprintUtil.parseOS(log.getUserAgent()))
                .deviceType(DeviceFingerprintUtil.parseDeviceType(log.getUserAgent()))
                .deviceFingerprint(DeviceFingerprintUtil.generateFingerprint(log.getUserAgent(), 
                        log.getIamIdentity() != null ? log.getIamIdentity().getIdentityName() : "system"))
                .awsService(log.getAwsService() != null ? log.getAwsService() : "sts")
                .apiCallName(log.getEventName())
                .dataVolumeBytes(log.getDataVolumeBytes() != null ? log.getDataVolumeBytes() : 0L)
                .loginHour(time.getHour())
                .loginDayOfWeek(time.getDayOfWeek().getValue())
                .isVpn(geo.isVpn())
                .isTor(geo.isTor())
                .isWeekend(time.getDayOfWeek().getValue() >= 6)
                .isMidnight(time.getHour() >= 0 && time.getHour() <= 5)
                .build();
    }

    @Override
    public BehaviorVector extractVector(UserSession session) {
        if (session == null) return null;

        LocalDateTime time = session.getLoginAt() != null ? session.getLoginAt() : LocalDateTime.now();
        GeoIpUtil.GeoLocation geo = GeoIpUtil.resolveIp(session.getIpAddress());

        String browser = "Unknown";
        String os = "Unknown";
        String deviceType = "DESKTOP";
        String fingerprint = "Unknown";

        if (session.getDevice() != null) {
            browser = session.getDevice().getBrowser();
            os = session.getDevice().getOperatingSystem();
            deviceType = session.getDevice().getDeviceType();
            fingerprint = session.getDevice().getDeviceFingerprint();
        }

        return BehaviorVector.builder()
                .identityArn(session.getUser() != null ? session.getUser().getUsername() : "web-user")
                .eventTime(time)
                .region("web-console")
                .countryCode(geo.getCountryCode())
                .sourceIp(session.getIpAddress())
                .userAgent("Console Login")
                .browser(browser)
                .os(os)
                .deviceType(deviceType)
                .deviceFingerprint(fingerprint)
                .awsService("signin.amazonaws.com")
                .apiCallName("ConsoleLogin")
                .dataVolumeBytes(0)
                .loginHour(time.getHour())
                .loginDayOfWeek(time.getDayOfWeek().getValue())
                .isVpn(geo.isVpn())
                .isTor(geo.isTor())
                .isWeekend(time.getDayOfWeek().getValue() >= 6)
                .isMidnight(time.getHour() >= 0 && time.getHour() <= 5)
                .build();
    }
}
