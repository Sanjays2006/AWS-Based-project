package com.cloudidentity.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AwsAccountResponse {

    private Long id;
    private String accountId;
    private String accountName;
    private String accountAlias;
    private String region;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
