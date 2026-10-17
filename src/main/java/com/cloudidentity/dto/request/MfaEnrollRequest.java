package com.cloudidentity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MfaEnrollRequest {

    @NotBlank(message = "MFA validation code is required")
    private String code;
}
