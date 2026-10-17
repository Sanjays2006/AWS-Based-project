package com.cloudidentity.controller;

import com.cloudidentity.service.CostDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/finops")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','ANALYST')")
public class CostDashboardController {

    private final CostDashboardService costDashboardService;

    @GetMapping("/summary")
    public ResponseEntity<CostDashboardService.FinOpsCostSummary> getFinOpsSummary() {
        return ResponseEntity.ok(costDashboardService.getCostSummary());
    }
}
