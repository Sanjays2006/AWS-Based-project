package com.cloudidentity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertNotificationResponse {
    private Long id;
    private Long anomalyEventId;
    private Long userId;
    private String channel;
    private String title;
    private String message;
    private boolean isRead;
    private LocalDateTime sentAt;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;
}
