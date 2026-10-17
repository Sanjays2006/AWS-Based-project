package com.cloudidentity.controller;

import com.cloudidentity.dto.request.ExportRequest;
import com.cloudidentity.service.ReportExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/v1/reports/export")
@RequiredArgsConstructor
public class ReportExportController {

    private final ReportExportService reportExportService;

    @PostMapping("/csv")
    public ResponseEntity<byte[]> exportCsv(@RequestBody(required = false) ExportRequest request) {
        if (request == null) request = new ExportRequest();
        byte[] csvBytes = reportExportService.generateCsvReport(request);

        String filename = "cloudidentity_report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvBytes);
    }
}
