package com.cloudidentity.controller;

import com.cloudidentity.dto.request.ResponsePolicyRequest;
import com.cloudidentity.dto.response.ResponsePolicyResponse;
import com.cloudidentity.service.ResponsePolicyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/response-policies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ResponsePolicyController {

    private final ResponsePolicyService responsePolicyService;

    @GetMapping
    public ResponseEntity<List<ResponsePolicyResponse>> getAllPolicies() {
        return ResponseEntity.ok(responsePolicyService.getAllPolicies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponsePolicyResponse> getPolicyById(@PathVariable Long id) {
        return ResponseEntity.ok(responsePolicyService.getPolicyById(id));
    }

    @PostMapping
    public ResponseEntity<ResponsePolicyResponse> createPolicy(@Valid @RequestBody ResponsePolicyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(responsePolicyService.createPolicy(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResponsePolicyResponse> updatePolicy(@PathVariable Long id, @Valid @RequestBody ResponsePolicyRequest request) {
        return ResponseEntity.ok(responsePolicyService.updatePolicy(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePolicy(@PathVariable Long id) {
        responsePolicyService.deletePolicy(id);
        return ResponseEntity.noContent().build();
    }
}
