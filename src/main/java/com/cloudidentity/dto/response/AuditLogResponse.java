package com.cloudidentity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private Long userId;
    private String username;
    private String action;
    private String entityType;
    private Long entityId;
    private String description;
    private String ipAddress;
    private String userAgent;
    private Map<String, Object> oldValues;
    private Map<String, Object> newValues;
    private LocalDateTime createdAt;
}
