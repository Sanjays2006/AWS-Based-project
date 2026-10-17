package com.cloudidentity.controller;

import com.cloudidentity.dto.request.SystemSettingRequest;
import com.cloudidentity.dto.response.SystemSettingResponse;
import com.cloudidentity.entity.User;
import com.cloudidentity.repository.UserRepository;
import com.cloudidentity.security.UserPrincipal;
import com.cloudidentity.service.SystemSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SystemSettingController {

    private final SystemSettingService systemSettingService;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<List<SystemSettingResponse>> getAllSettings() {
        return ResponseEntity.ok(systemSettingService.getAllSettings());
    }

    @PutMapping("/{key}")
    public ResponseEntity<SystemSettingResponse> updateSetting(@PathVariable String key,
                                                               @Valid @RequestBody SystemSettingRequest request,
                                                               @AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        return ResponseEntity.ok(systemSettingService.updateSetting(key, request.getValue(), user));
    }
}
