package com.cloudidentity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemSettingResponse {
    private Long id;
    private String settingKey;
    private String settingValue;
    private String valueType;
    private String description;
    private String updatedByUsername;
    private LocalDateTime updatedAt;
}
