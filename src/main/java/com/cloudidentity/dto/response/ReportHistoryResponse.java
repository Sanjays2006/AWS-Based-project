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
public class ReportHistoryResponse {
    private Long id;
    private String reportType;
    private Long generatedByUserId;
    private String generatedByUsername;
    private Map<String, Object> parameters;
    private Integer recordCount;
    private String filePath;
    private LocalDateTime generatedAt;
}
