package com.cloudidentity.service;

import com.cloudidentity.entity.AnomalyEvent;
import com.cloudidentity.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class EmailService {

    public void sendAlertEmail(User user, AnomalyEvent anomaly, String title, String message) {
        String recipientEmail = (user != null && user.getEmail() != null) ? user.getEmail() : "admin@cloudidentity.local";
        String severity = (anomaly != null && anomaly.getSeverity() != null) ? anomaly.getSeverity() : "HIGH";
        Long anomalyId = (anomaly != null) ? anomaly.getId() : -1L;

        log.info("======================== [EMAIL ALERT SENT] ========================");
        log.info("TO: {}", recipientEmail);
        log.info("SUBJECT: [{}] {}", severity, title);
        log.info("BODY:\nHello {},\n\nA security anomaly has been detected in your monitored AWS environment.\n\n" +
                        "Severity: {}\nAnomaly ID: {}\nMessage: {}\n\nPlease log in to the Cloud Identity Security Portal to review and take action.\n" +
                        "====================================================================",
                user != null ? user.getUsername() : "Administrator",
                severity, anomalyId, message);
    }

    public void sendDailyDigestEmail(User user, String summaryContent) {
        String recipientEmail = (user != null && user.getEmail() != null) ? user.getEmail() : "admin@cloudidentity.local";

        log.info("======================== [DAILY SECURITY DIGEST SENT] ========================");
        log.info("TO: {}", recipientEmail);
        log.info("SUBJECT: Cloud Identity Daily Security Summary");
        log.info("BODY:\nHello {},\n\nHere is your daily security summary:\n{}\n\nStay secure,\nCloud Identity Engine Team",
                user != null ? user.getUsername() : "Administrator", summaryContent);
        log.info("==============================================================================");
    }
}
