package com.cloudidentity.service;

import com.cloudidentity.dto.request.ResponsePolicyRequest;
import com.cloudidentity.dto.response.ResponsePolicyResponse;
import com.cloudidentity.entity.ResponsePolicy;
import com.cloudidentity.exception.ResourceNotFoundException;
import com.cloudidentity.repository.ResponsePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResponsePolicyService {

    private final ResponsePolicyRepository responsePolicyRepository;

    @Transactional(readOnly = true)
    public List<ResponsePolicyResponse> getAllPolicies() {
        return responsePolicyRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ResponsePolicyResponse getPolicyById(Long id) {
        ResponsePolicy policy = responsePolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Response Policy not found with ID: " + id));
        return toResponse(policy);
    }

    @Transactional
    public ResponsePolicyResponse createPolicy(ResponsePolicyRequest request) {
        ResponsePolicy policy = ResponsePolicy.builder()
                .policyName(request.getPolicyName())
                .riskLevelTrigger(request.getRiskLevelTrigger())
                .minRiskScore(request.getMinRiskScore())
                .responseAction(request.getResponseAction())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .priority(request.getPriority() != null ? request.getPriority() : 1)
                .build();

        return toResponse(responsePolicyRepository.save(policy));
    }

    @Transactional
    public ResponsePolicyResponse updatePolicy(Long id, ResponsePolicyRequest request) {
        ResponsePolicy policy = responsePolicyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Response Policy not found with ID: " + id));

        policy.setPolicyName(request.getPolicyName());
        policy.setRiskLevelTrigger(request.getRiskLevelTrigger());
        policy.setMinRiskScore(request.getMinRiskScore());
        policy.setResponseAction(request.getResponseAction());
        policy.setDescription(request.getDescription());
        if (request.getIsActive() != null) policy.setIsActive(request.getIsActive());
        if (request.getPriority() != null) policy.setPriority(request.getPriority());

        return toResponse(responsePolicyRepository.save(policy));
    }

    @Transactional
    public void deletePolicy(Long id) {
        if (!responsePolicyRepository.existsById(id)) {
            throw new ResourceNotFoundException("Response Policy not found with ID: " + id);
        }
        responsePolicyRepository.deleteById(id);
    }

    private ResponsePolicyResponse toResponse(ResponsePolicy policy) {
        return ResponsePolicyResponse.builder()
                .id(policy.getId())
                .policyName(policy.getPolicyName())
                .riskLevelTrigger(policy.getRiskLevelTrigger())
                .minRiskScore(policy.getMinRiskScore())
                .responseAction(policy.getResponseAction())
                .description(policy.getDescription())
                .isActive(policy.getIsActive())
                .priority(policy.getPriority())
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt())
                .build();
    }
}
