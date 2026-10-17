package com.cloudidentity.controller;

import com.cloudidentity.dto.request.RiskRuleRequest;
import com.cloudidentity.dto.response.RiskRuleResponse;
import com.cloudidentity.entity.RiskRule;
import com.cloudidentity.exception.ResourceNotFoundException;
import com.cloudidentity.repository.RiskRuleRepository;
import com.cloudidentity.util.MapperUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/risk-rules")
@RequiredArgsConstructor
public class RiskRuleController {

    private final RiskRuleRepository riskRuleRepository;
    private final MapperUtil mapperUtil;

    @GetMapping
    public ResponseEntity<List<RiskRuleResponse>> listRules() {
        return ResponseEntity.ok(riskRuleRepository.findAll().stream()
                .map(mapperUtil::toRiskRuleResponse).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RiskRuleResponse> getRuleById(@PathVariable Long id) {
        RiskRule rule = riskRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Risk rule not found with id: " + id));
        return ResponseEntity.ok(mapperUtil.toRiskRuleResponse(rule));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RiskRuleResponse> updateRule(@PathVariable Long id,
                                                       @Valid @RequestBody RiskRuleRequest request) {
        RiskRule rule = riskRuleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Risk rule not found with id: " + id));

        rule.setWeight(request.getWeight());
        if (request.getIsActive() != null) {
            rule.setIsActive(request.getIsActive());
        }

        riskRuleRepository.save(rule);
        return ResponseEntity.ok(mapperUtil.toRiskRuleResponse(rule));
    }
}
