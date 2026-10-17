package com.cloudidentity.controller;

import com.cloudidentity.dto.response.BehavioralDriftResponse;
import com.cloudidentity.dto.response.TopRiskyApiResponse;
import com.cloudidentity.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/drift/{identityId}")
    public ResponseEntity<List<BehavioralDriftResponse>> getBehavioralDrift(
            @PathVariable Long identityId,
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        return ResponseEntity.ok(analyticsService.getBehavioralDriftByIdentity(identityId, days));
    }

    @GetMapping("/top-risky-apis")
    public ResponseEntity<List<TopRiskyApiResponse>> getTopRiskyApis(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return ResponseEntity.ok(analyticsService.getTopRiskyApis(limit));
    }

    @GetMapping("/risk-distribution")
    public ResponseEntity<Map<String, Long>> getRiskDistribution() {
        return ResponseEntity.ok(analyticsService.getRiskDistribution());
    }
}
