package com.cloudidentity.service;

import com.cloudidentity.dto.request.ExportRequest;
import com.cloudidentity.entity.AnomalyEvent;
import com.cloudidentity.repository.AnomalyEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReportExportService {

    private final AnomalyEventRepository anomalyEventRepository;

    @Transactional(readOnly = true)
    public byte[] generateCsvReport(ExportRequest request) {
        StringBuilder csv = new StringBuilder();
        csv.append("Anomaly ID,Identity Name,Severity,Anomaly Type,Risk Score,Description,Created At\n");

        List<AnomalyEvent> anomalies = anomalyEventRepository.findAll();
        for (AnomalyEvent a : anomalies) {
            csv.append(a.getId()).append(",")
               .append(escapeCsv(a.getIamIdentity() != null ? a.getIamIdentity().getIdentityName() : "Unknown")).append(",")
               .append(escapeCsv(a.getSeverity())).append(",")
               .append(escapeCsv(a.getAnomalyType())).append(",")
               .append(a.getRiskDelta() != null ? a.getRiskDelta() : 0.0).append(",")
               .append(escapeCsv(a.getDescription())).append(",")
               .append(a.getCreatedAt()).append("\n");
        }

        return csv.toString().getBytes();
    }

    private String escapeCsv(String val) {
        if (val == null) return "\"\"";
        return "\"" + val.replace("\"", "\"\"") + "\"";
    }
}
