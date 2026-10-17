package com.cloudidentity.controller;

import com.cloudidentity.service.SimulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/simulation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SimulationController {

    private final SimulationService simulationService;

    @GetMapping("/scenarios")
    public ResponseEntity<List<Map<String, Object>>> getScenarios() {
        return ResponseEntity.ok(simulationService.getAvailableScenarios());
    }

    @PostMapping("/run/{scenarioCode}")
    public ResponseEntity<Map<String, Object>> runSimulation(
            @PathVariable String scenarioCode,
            @RequestParam(required = false) Long identityId) {
        return ResponseEntity.ok(simulationService.runSimulation(scenarioCode, identityId));
    }
}
