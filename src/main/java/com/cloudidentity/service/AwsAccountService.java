package com.cloudidentity.service;

import com.cloudidentity.dto.request.AwsAccountRequest;
import com.cloudidentity.dto.response.AwsAccountResponse;
import com.cloudidentity.entity.AwsAccount;
import com.cloudidentity.exception.DuplicateResourceException;
import com.cloudidentity.exception.ResourceNotFoundException;
import com.cloudidentity.repository.AwsAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AwsAccountService {

    private final AwsAccountRepository awsAccountRepository;

    @Transactional(readOnly = true)
    public List<AwsAccountResponse> getAllAccounts() {
        return awsAccountRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AwsAccountResponse getAccountById(Long id) {
        AwsAccount account = awsAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AWS Account not found with ID: " + id));
        return toResponse(account);
    }

    @Transactional
    public AwsAccountResponse createAccount(AwsAccountRequest request) {
        if (awsAccountRepository.findByAccountId(request.getAccountId()).isPresent()) {
            throw new DuplicateResourceException("AWS Account with ID " + request.getAccountId() + " already exists.");
        }

        AwsAccount account = AwsAccount.builder()
                .accountId(request.getAccountId())
                .accountName(request.getAccountName())
                .accountAlias(request.getAccountAlias())
                .region(request.getRegion() != null ? request.getRegion() : "us-east-1")
                .accessKeyId(request.getAccessKeyId())
                .secretAccessKey(request.getSecretAccessKey())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return toResponse(awsAccountRepository.save(account));
    }

    @Transactional
    public AwsAccountResponse updateAccount(Long id, AwsAccountRequest request) {
        AwsAccount account = awsAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AWS Account not found with ID: " + id));

        account.setAccountName(request.getAccountName());
        account.setAccountAlias(request.getAccountAlias());
        account.setRegion(request.getRegion());
        if (request.getAccessKeyId() != null) account.setAccessKeyId(request.getAccessKeyId());
        if (request.getSecretAccessKey() != null) account.setSecretAccessKey(request.getSecretAccessKey());
        if (request.getIsActive() != null) account.setIsActive(request.getIsActive());

        return toResponse(awsAccountRepository.save(account));
    }

    @Transactional
    public void deleteAccount(Long id) {
        if (!awsAccountRepository.existsById(id)) {
            throw new ResourceNotFoundException("AWS Account not found with ID: " + id);
        }
        awsAccountRepository.deleteById(id);
    }

    private AwsAccountResponse toResponse(AwsAccount account) {
        return AwsAccountResponse.builder()
                .id(account.getId())
                .accountId(account.getAccountId())
                .accountName(account.getAccountName())
                .accountAlias(account.getAccountAlias())
                .region(account.getRegion())
                .isActive(account.getIsActive())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }
}
