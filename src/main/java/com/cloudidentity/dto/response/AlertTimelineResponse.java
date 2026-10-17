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
public class AlertTimelineResponse {
    private Long id;
    private String type; // ANOMALY | AUTOMATED_RESPONSE | SYSTEM_AUDIT
    private String title;
    private String description;
    private String severity;
    private LocalDateTime timestamp;
    private Long referenceId;
}
