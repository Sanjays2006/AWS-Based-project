package com.cloudidentity.controller;

import com.cloudidentity.dto.request.AwsAccountRequest;
import com.cloudidentity.dto.response.AwsAccountResponse;
import com.cloudidentity.service.AwsAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/aws-accounts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AwsAccountController {

    private final AwsAccountService awsAccountService;

    @GetMapping
    public ResponseEntity<List<AwsAccountResponse>> getAllAccounts() {
        return ResponseEntity.ok(awsAccountService.getAllAccounts());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AwsAccountResponse> getAccountById(@PathVariable Long id) {
        return ResponseEntity.ok(awsAccountService.getAccountById(id));
    }

    @PostMapping
    public ResponseEntity<AwsAccountResponse> createAccount(@Valid @RequestBody AwsAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(awsAccountService.createAccount(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AwsAccountResponse> updateAccount(@PathVariable Long id, @Valid @RequestBody AwsAccountRequest request) {
        return ResponseEntity.ok(awsAccountService.updateAccount(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        awsAccountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
