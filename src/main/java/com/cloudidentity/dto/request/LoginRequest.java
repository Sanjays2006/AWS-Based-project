package com.cloudidentity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    private String mfaCode;

    private String screenResolution;

    private String timezone;
}
