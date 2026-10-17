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
public class AutomatedResponseResponse {
    private Long id;
    private Long anomalyEventId;
    private Long iamIdentityId;
    private String identityArn;
    private String responseType;
    private String status;
    private Map<String, Object> executionDetails;
    private String executedBy;
    private LocalDateTime executedAt;
    private LocalDateTime createdAt;
}
