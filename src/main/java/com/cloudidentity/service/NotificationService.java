package com.cloudidentity.service;

import com.cloudidentity.dto.response.AlertNotificationResponse;
import com.cloudidentity.dto.response.PagedResponse;
import com.cloudidentity.entity.AlertNotification;
import com.cloudidentity.entity.AnomalyEvent;
import com.cloudidentity.entity.User;
import com.cloudidentity.exception.ResourceNotFoundException;
import com.cloudidentity.repository.AlertNotificationRepository;
import com.cloudidentity.util.MapperUtil;
import com.cloudidentity.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final AlertNotificationRepository alertNotificationRepository;
    private final MapperUtil mapperUtil;
    private final EmailService emailService;
    private final SnsClient snsClient;

    @Value("${aws.sns.topic-arn:}")
    private String snsTopicArn;

    @Transactional(readOnly = true)
    public List<AlertNotificationResponse> getUnreadNotifications(Long userId) {
        return alertNotificationRepository.findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream().map(mapperUtil::toAlertNotificationResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PagedResponse<AlertNotificationResponse> listNotifications(Long userId, int page, int size) {
        Pageable pageable = PaginationUtil.createPageable(page, size, "createdAt", "DESC");
        Page<AlertNotification> notesPage = alertNotificationRepository.findByUser_IdOrderByCreatedAtDesc(userId, pageable);

        return PaginationUtil.toPagedResponse(notesPage,
                notesPage.getContent().stream().map(mapperUtil::toAlertNotificationResponse).collect(Collectors.toList()));
    }

    @Transactional
    public void markAsRead(Long id, Long userId) {
        AlertNotification note = alertNotificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));

        if (!note.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("Unauthorized modification of notification record");
        }

        note.setIsRead(true);
        note.setReadAt(LocalDateTime.now());
        alertNotificationRepository.save(note);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<AlertNotification> unread = alertNotificationRepository.findByUser_IdAndIsReadFalseOrderByCreatedAtDesc(userId);
        LocalDateTime now = LocalDateTime.now();
        for (AlertNotification note : unread) {
            note.setIsRead(true);
            note.setReadAt(now);
        }
        alertNotificationRepository.saveAll(unread);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return alertNotificationRepository.countByUser_IdAndIsReadFalse(userId);
    }

    @Transactional
    public void createNotification(User user, AnomalyEvent anomaly, String title, String message, String channel) {
        AlertNotification note = AlertNotification.builder()
                .user(user)
                .anomalyEvent(anomaly)
                .channel(channel != null ? channel : "IN_APP")
                .title(title)
                .message(message)
                .isRead(false)
                .sentAt(LocalDateTime.now())
                .build();
        alertNotificationRepository.save(note);

        if (emailService != null) {
            emailService.sendAlertEmail(user, anomaly, title, message);
        }

        // Fan out high-signal alerts to AWS SNS (ops channel / email / SMS subscribers).
        publishToSns(title, message);
    }

    /**
     * Publishes an alert to the configured AWS SNS topic. No-op (with a debug
     * log) when {@code aws.sns.topic-arn} is not configured, so local/offline
     * runs are unaffected. Failures are logged but never propagated — an SNS
     * outage must not break in-app notification persistence.
     */
    private void publishToSns(String subject, String message) {
        if (snsTopicArn == null || snsTopicArn.isBlank()) {
            log.debug("[AWS SNS] topic ARN not configured — skipping SNS publish.");
            return;
        }
        try {
            // SNS subject max length is 100 chars and must be ASCII.
            String safeSubject = subject == null ? "Security Alert"
                    : subject.replaceAll("[^\\x20-\\x7E]", "").trim();
            if (safeSubject.length() > 100) {
                safeSubject = safeSubject.substring(0, 100);
            }

            PublishResponse response = snsClient.publish(PublishRequest.builder()
                    .topicArn(snsTopicArn)
                    .subject(safeSubject.isBlank() ? "Security Alert" : safeSubject)
                    .message(message != null ? message : safeSubject)
                    .build());
            log.info("[AWS SNS] Alert published to topic (messageId={})", response.messageId());
        } catch (Exception ex) {
            log.warn("[AWS SNS] Failed to publish alert to topic '{}': {}", snsTopicArn, ex.getMessage());
        }
    }
}
