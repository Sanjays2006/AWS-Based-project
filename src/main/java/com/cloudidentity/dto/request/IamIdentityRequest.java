package com.cloudidentity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IamIdentityRequest {

    @NotBlank(message = "AWS ARN is required")
    @Pattern(regexp = "^arn:aws:iam::\\d{12}:(user|role|group)/[a-zA-Z0-9+=,.@\\-_/]+$", 
             message = "Invalid AWS IAM ARN format")
    private String arn;

    @NotBlank(message = "Identity type is required")
    @Pattern(regexp = "USER|ROLE|GROUP", message = "Type must be USER, ROLE, or GROUP")
    private String identityType;

    private Boolean isMonitored;
}
