package com.cloudidentity.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingRequest {

    @NotBlank(message = "Setting value cannot be blank")
    private String value;
}
