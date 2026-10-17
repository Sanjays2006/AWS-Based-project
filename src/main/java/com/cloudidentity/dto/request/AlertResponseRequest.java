package com.cloudidentity.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponseRequest {

    @NotBlank(message = "Response action is required")
    @Pattern(regexp = "WAS_ME|NOT_ME", message = "Response action must be WAS_ME or NOT_ME")
    private String response;
}
