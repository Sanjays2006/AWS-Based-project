package com.cloudidentity.dto.request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ExportRequest {

    private String reportType = "DAILY_SUMMARY"; // DAILY_SUMMARY | ANOMALY_EXECUTIVE | AUDIT_COMPLIANCE
    private String format = "CSV"; // CSV | PDF
    private LocalDate startDate;
    private LocalDate endDate;
    private String severityFilter;
}
