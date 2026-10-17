package com.cloudidentity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AwsAccountRequest {

    @NotBlank(message = "AWS Account ID is required")
    @Size(min = 12, max = 12, message = "AWS Account ID must be exactly 12 digits")
    private String accountId;

    @NotBlank(message = "Account name is required")
    private String accountName;

    private String accountAlias;

    private String region = "us-east-1";

    private String accessKeyId;

    private String secretAccessKey;

    private Boolean isActive = true;
}
