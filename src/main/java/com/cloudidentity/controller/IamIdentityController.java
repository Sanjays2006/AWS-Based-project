package com.cloudidentity.controller;

import com.cloudidentity.dto.request.IamIdentityRequest;
import com.cloudidentity.dto.response.IamIdentityResponse;
import com.cloudidentity.dto.response.PagedResponse;
import com.cloudidentity.entity.IamIdentity;
import com.cloudidentity.exception.DuplicateResourceException;
import com.cloudidentity.exception.ResourceNotFoundException;
import com.cloudidentity.repository.IamIdentityRepository;
import com.cloudidentity.util.MapperUtil;
import com.cloudidentity.util.PaginationUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/iam-identities")
@RequiredArgsConstructor
public class IamIdentityController {

    private final IamIdentityRepository iamIdentityRepository;
    private final MapperUtil mapperUtil;

    @GetMapping
    public ResponseEntity<PagedResponse<IamIdentityResponse>> listIdentities(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<IamIdentity> identityPage = iamIdentityRepository.searchIdentities(search,
                PaginationUtil.createPageable(page, size, "createdAt", "DESC"));

        return ResponseEntity.ok(PaginationUtil.toPagedResponse(identityPage,
                identityPage.getContent().stream().map(mapperUtil::toIamIdentityResponse).collect(Collectors.toList())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<IamIdentityResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mapperUtil.toIamIdentityResponse(iamIdentityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IAM Identity not found: " + id))));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<IamIdentityResponse> create(@Valid @RequestBody IamIdentityRequest request) {
        if (iamIdentityRepository.findByArn(request.getArn()).isPresent()) {
            throw new DuplicateResourceException("IAM Identity with ARN already exists: " + request.getArn());
        }
        IamIdentity identity = IamIdentity.builder()
                .arn(request.getArn())
                .identityType(request.getIdentityType())
                .isMonitored(request.getIsMonitored() != null ? request.getIsMonitored() : true)
                .build();
        iamIdentityRepository.save(identity);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapperUtil.toIamIdentityResponse(identity));
    }

    @PatchMapping("/{id}/toggle-monitor")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> toggleMonitoring(@PathVariable Long id) {
        IamIdentity identity = iamIdentityRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("IAM Identity not found: " + id));
        identity.setIsMonitored(!identity.getIsMonitored());
        iamIdentityRepository.save(identity);
        return ResponseEntity.ok(Map.of("message", "Monitoring status toggled to: " + identity.getIsMonitored()));
    }
}
