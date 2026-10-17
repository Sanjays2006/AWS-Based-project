package com.cloudidentity.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskTrendResponse {
    private List<RiskTrendPoint> points;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RiskTrendPoint {
        private LocalDate date;
        private double averageScore;
        private double maxScore;
        private long criticalCount;
    }
}
