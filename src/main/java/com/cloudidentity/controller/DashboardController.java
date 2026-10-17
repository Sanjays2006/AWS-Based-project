package com.cloudidentity.controller;

import com.cloudidentity.dto.response.*;
import com.cloudidentity.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/heatmap")
    public ResponseEntity<HeatmapDataResponse> getHeatmap() {
        return ResponseEntity.ok(dashboardService.getHeatmapData());
    }

    @GetMapping("/world-map")
    public ResponseEntity<WorldMapDataResponse> getWorldMap() {
        return ResponseEntity.ok(dashboardService.getWorldMapData());
    }

    @GetMapping("/risk-trend")
    public ResponseEntity<RiskTrendResponse> getRiskTrend(
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(dashboardService.getRiskTrend(days));
    }

    @GetMapping("/service-usage")
    public ResponseEntity<Map<String, Long>> getServiceUsage() {
        return ResponseEntity.ok(dashboardService.getServiceUsage());
    }
}
