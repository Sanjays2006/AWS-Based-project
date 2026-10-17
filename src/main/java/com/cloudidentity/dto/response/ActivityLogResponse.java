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
public class ActivityLogResponse {
    private Long id;
    private Long iamIdentityId;
    private String identityName;
    private String identityArn;
    private String eventName;
    private String eventSource;
    private LocalDateTime eventTime;
    private String sourceIp;
    private String userAgent;
    private String region;
    private String awsService;
    private Map<String, Object> requestParameters;
    private Map<String, Object> responseElements;
    private Long dataVolumeBytes;
    private LocalDateTime ingestedAt;
}
