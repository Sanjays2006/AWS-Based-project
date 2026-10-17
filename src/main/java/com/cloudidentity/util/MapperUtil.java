package com.cloudidentity.util;

import com.cloudidentity.dto.response.*;
import com.cloudidentity.entity.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MapperUtil {

    public UserResponse toUserResponse(User user) {
        if (user == null) return null;
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .isActive(user.getIsActive())
                .mfaEnabled(user.getMfaEnabled())
                .lastLoginAt(user.getLastLoginAt())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    public SessionResponse toSessionResponse(UserSession session) {
        if (session == null) return null;
        return SessionResponse.builder()
                .id(session.getId())
                .userId(session.getUser() != null ? session.getUser().getId() : null)
                .ipAddress(session.getIpAddress())
                .browser(session.getDevice() != null ? session.getDevice().getBrowser() : "Unknown")
                .operatingSystem(session.getDevice() != null ? session.getDevice().getOperatingSystem() : "Unknown")
                .deviceType(session.getDevice() != null ? session.getDevice().getDeviceType() : "Unknown")
                .loginAt(session.getLoginAt())
                .logoutAt(session.getLogoutAt())
                .lastActivityAt(session.getLastActivityAt())
                .durationMinutes(session.getDurationMinutes())
                .isActive(session.getIsActive())
                .build();
    }

    public DeviceResponse toDeviceResponse(Device device) {
        if (device == null) return null;
        return DeviceResponse.builder()
                .id(device.getId())
                .deviceFingerprint(device.getDeviceFingerprint())
                .browser(device.getBrowser())
                .operatingSystem(device.getOperatingSystem())
                .deviceType(device.getDeviceType())
                .isTrusted(device.getIsTrusted())
                .firstSeenAt(device.getFirstSeenAt())
                .lastSeenAt(device.getLastSeenAt())
                .build();
    }

    public LoginLocationResponse toLoginLocationResponse(LoginLocation loc) {
        if (loc == null) return null;
        return LoginLocationResponse.builder()
                .id(loc.getId())
                .ipAddress(loc.getIpAddress())
                .countryCode(loc.getCountryCode())
                .countryName(loc.getCountryName())
                .city(loc.getCity())
                .latitude(loc.getLatitude())
                .longitude(loc.getLongitude())
                .isVpn(loc.getIsVpn())
                .isTor(loc.getIsTor())
                .loginAt(loc.getLoginAt())
                .build();
    }

    public IamIdentityResponse toIamIdentityResponse(IamIdentity identity) {
        if (identity == null) return null;
        return IamIdentityResponse.builder()
                .id(identity.getId())
                .arn(identity.getArn())
                .identityType(identity.getIdentityType())
                .accountId(identity.getAccountId())
                .identityName(identity.getIdentityName())
                .region(identity.getRegion())
                .isMonitored(identity.getIsMonitored())
                .createdAt(identity.getCreatedAt())
                .updatedAt(identity.getUpdatedAt())
                .build();
    }

    @SuppressWarnings("unchecked")
    public ActivityLogResponse toActivityLogResponse(ActivityLog log) {
        if (log == null) return null;
        return ActivityLogResponse.builder()
                .id(log.getId())
                .iamIdentityId(log.getIamIdentity() != null ? log.getIamIdentity().getId() : null)
                .identityName(log.getIamIdentity() != null ? log.getIamIdentity().getIdentityName() : "Unknown")
                .identityArn(log.getIamIdentity() != null ? log.getIamIdentity().getArn() : "Unknown")
                .eventName(log.getEventName())
                .eventSource(log.getEventSource())
                .eventTime(log.getEventTime())
                .sourceIp(log.getSourceIp())
                .userAgent(log.getUserAgent())
                .region(log.getRegion())
                .awsService(log.getAwsService())
                .requestParameters(log.getRequestParameters())
                .responseElements(log.getResponseElements())
                .dataVolumeBytes(log.getDataVolumeBytes())
                .ingestedAt(log.getIngestedAt())
                .build();
    }

    public AuditLogResponse toAuditLogResponse(AuditLog log) {
        if (log == null) return null;
        return AuditLogResponse.builder()
                .id(log.getId())
                .userId(log.getUser() != null ? log.getUser().getId() : null)
                .username(log.getUser() != null ? log.getUser().getUsername() : "SYSTEM")
                .action(log.getAction())
                .entityType(log.getEntityType())
                .entityId(log.getEntityId())
                .description(log.getDescription())
                .ipAddress(log.getIpAddress())
                .userAgent(log.getUserAgent())
                .oldValues(log.getOldValues())
                .newValues(log.getNewValues())
                .createdAt(log.getCreatedAt())
                .build();
    }

    @SuppressWarnings("unchecked")
    public RiskScoreResponse toRiskScoreResponse(RiskScore score) {
        if (score == null) return null;
        return RiskScoreResponse.builder()
                .id(score.getId())
                .iamIdentityId(score.getIamIdentity() != null ? score.getIamIdentity().getId() : null)
                .identityName(score.getIamIdentity() != null ? score.getIamIdentity().getIdentityName() : "Unknown")
                .identityArn(score.getIamIdentity() != null ? score.getIamIdentity().getArn() : "Unknown")
                .score(score.getScore())
                .riskLevel(score.getRiskLevel())
                .triggeredRules(score.getTriggeredRules() != null ? score.getTriggeredRules() : Collections.emptyList())
                .scoreFactors(score.getScoreFactors() != null ? score.getScoreFactors() : Collections.emptyMap())
                .computedAt(score.getComputedAt())
                .build();
    }

    public RiskRuleResponse toRiskRuleResponse(RiskRule rule) {
        if (rule == null) return null;
        return RiskRuleResponse.builder()
                .id(rule.getId())
                .ruleCode(rule.getRuleCode())
                .description(rule.getDescription())
                .weight(rule.getWeight())
                .category(rule.getCategory())
                .isActive(rule.getIsActive())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }

    public AnomalyEventResponse toAnomalyEventResponse(AnomalyEvent event) {
        if (event == null) return null;
        return AnomalyEventResponse.builder()
                .id(event.getId())
                .iamIdentityId(event.getIamIdentity() != null ? event.getIamIdentity().getId() : null)
                .identityName(event.getIamIdentity() != null ? event.getIamIdentity().getIdentityName() : "Unknown")
                .identityArn(event.getIamIdentity() != null ? event.getIamIdentity().getArn() : "Unknown")
                .activityLogId(event.getActivityLog() != null ? event.getActivityLog().getId() : null)
                .anomalyType(event.getAnomalyType())
                .severity(event.getSeverity())
                .description(event.getDescription())
                .confidenceScore(event.getConfidenceScore())
                .riskDelta(event.getRiskDelta())
                .isAcknowledged(event.getIsAcknowledged())
                .acknowledgedByUserId(event.getAcknowledgedByUser() != null ? event.getAcknowledgedByUser().getId() : null)
                .acknowledgedByUsername(event.getAcknowledgedByUser() != null ? event.getAcknowledgedByUser().getUsername() : null)
                .acknowledgedAt(event.getAcknowledgedAt())
                .alertResponse(event.getAlertResponse())
                .createdAt(event.getCreatedAt())
                .build();
    }

    public AlertNotificationResponse toAlertNotificationResponse(AlertNotification note) {
        if (note == null) return null;
        return AlertNotificationResponse.builder()
                .id(note.getId())
                .anomalyEventId(note.getAnomalyEvent() != null ? note.getAnomalyEvent().getId() : null)
                .userId(note.getUser() != null ? note.getUser().getId() : null)
                .channel(note.getChannel())
                .title(note.getTitle())
                .message(note.getMessage())
                .isRead(note.getIsRead())
                .sentAt(note.getSentAt())
                .readAt(note.getReadAt())
                .createdAt(note.getCreatedAt())
                .build();
    }

    public AutomatedResponseResponse toAutomatedResponseResponse(AutomatedResponse resp) {
        if (resp == null) return null;
        return AutomatedResponseResponse.builder()
                .id(resp.getId())
                .anomalyEventId(resp.getAnomalyEvent() != null ? resp.getAnomalyEvent().getId() : null)
                .iamIdentityId(resp.getIamIdentity() != null ? resp.getIamIdentity().getId() : null)
                .identityArn(resp.getIamIdentity() != null ? resp.getIamIdentity().getArn() : "Unknown")
                .responseType(resp.getResponseType())
                .status(resp.getStatus())
                .executionDetails(resp.getExecutionDetails())
                .executedBy(resp.getExecutedBy())
                .executedAt(resp.getExecutedAt())
                .createdAt(resp.getCreatedAt())
                .build();
    }

    public ReportHistoryResponse toReportHistoryResponse(ReportHistory rpt) {
        if (rpt == null) return null;
        return ReportHistoryResponse.builder()
                .id(rpt.getId())
                .reportType(rpt.getReportType())
                .generatedByUserId(rpt.getGeneratedBy() != null ? rpt.getGeneratedBy().getId() : null)
                .generatedByUsername(rpt.getGeneratedBy() != null ? rpt.getGeneratedBy().getUsername() : "SYSTEM")
                .parameters(rpt.getParameters())
                .recordCount(rpt.getRecordCount())
                .filePath(rpt.getFilePath())
                .generatedAt(rpt.getGeneratedAt())
                .build();
    }

    public SystemSettingResponse toSystemSettingResponse(SystemSetting setting) {
        if (setting == null) return null;
        return SystemSettingResponse.builder()
                .id(setting.getId())
                .settingKey(setting.getSettingKey())
                .settingValue(setting.getSettingValue())
                .valueType(setting.getValueType())
                .description(setting.getDescription())
                .updatedByUsername(setting.getUpdatedBy() != null ? setting.getUpdatedBy().getUsername() : "SYSTEM")
                .updatedAt(setting.getUpdatedAt())
                .build();
    }

    @SuppressWarnings("unchecked")
    public BehaviorFingerprintResponse toBehaviorFingerprintResponse(BehaviorFingerprint fp) {
        if (fp == null) return null;
        return BehaviorFingerprintResponse.builder()
                .id(fp.getId())
                .iamIdentityId(fp.getIamIdentity() != null ? fp.getIamIdentity().getId() : null)
                .identityName(fp.getIamIdentity() != null ? fp.getIamIdentity().getIdentityName() : "Unknown")
                .identityArn(fp.getIamIdentity() != null ? fp.getIamIdentity().getArn() : "Unknown")
                .typicalHoursStart(fp.getTypicalHoursStart())
                .typicalHoursEnd(fp.getTypicalHoursEnd())
                .typicalWeekdays(fp.getTypicalWeekdays())
                .typicalRegions(fp.getTypicalRegions())
                .typicalCountries(fp.getTypicalCountries())
                .typicalIpRanges(fp.getTypicalIpRanges())
                .typicalDeviceFingerprints(fp.getTypicalDeviceFingerprints())
                .typicalBrowsers(fp.getTypicalBrowsers())
                .typicalOsTypes(fp.getTypicalOsTypes())
                .typicalServices(fp.getTypicalServices())
                .typicalApiCalls(fp.getTypicalApiCalls())
                .typicalResourceSequences(fp.getTypicalResourceSequences())
                .avgDailyApiCalls(fp.getAvgDailyApiCalls())
                .avgDataDownloadBytes(fp.getAvgDataDownloadBytes())
                .maxDataDownloadBytes(fp.getMaxDataDownloadBytes())
                .avgSessionDurationMinutes(fp.getAvgSessionDurationMinutes())
                .avgLoginsPerDay(fp.getAvgLoginsPerDay())
                .avgFailedLoginsPerDay(fp.getAvgFailedLoginsPerDay())
                .avgPrivilegeEscalationsPerWeek(fp.getAvgPrivilegeEscalationsPerWeek())
                .knownHighPrivilegeActions(fp.getKnownHighPrivilegeActions())
                .baselineComputedAt(fp.getBaselineComputedAt())
                .updatedAt(fp.getUpdatedAt())
                .build();
    }
}
