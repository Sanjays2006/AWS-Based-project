package com.cloudidentity.controller;

import com.cloudidentity.dto.request.AlertResponseRequest;
import com.cloudidentity.dto.response.AnomalyEventResponse;
import com.cloudidentity.dto.response.DeviceResponse;
import com.cloudidentity.dto.response.SessionResponse;
import com.cloudidentity.entity.User;
import com.cloudidentity.repository.UserRepository;
import com.cloudidentity.security.UserPrincipal;
import com.cloudidentity.service.SessionService;
import com.cloudidentity.service.SelfServicePortalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/self-service")
@RequiredArgsConstructor
public class SelfServiceController {

    private final SelfServicePortalService selfServicePortalService;
    private final SessionService sessionService;
    private final UserRepository userRepository;

    @GetMapping("/my-alerts")
    public ResponseEntity<List<AnomalyEventResponse>> getMyAlerts(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        return ResponseEntity.ok(selfServicePortalService.getMyAlerts(user));
    }

    @PostMapping("/alerts/{alertId}/respond")
    public ResponseEntity<Map<String, String>> respondToAlert(@PathVariable Long alertId,
                                                              @Valid @RequestBody AlertResponseRequest request,
                                                              @AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        selfServicePortalService.respondToAlert(alertId, user, request.getResponse());
        return ResponseEntity.ok(Map.of("message", "Alert response recorded: " + request.getResponse()));
    }

    @GetMapping("/my-sessions")
    public ResponseEntity<List<SessionResponse>> getMySessions(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        return ResponseEntity.ok(selfServicePortalService.getMySessions(user));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Map<String, String>> terminateSession(@PathVariable Long sessionId) {
        sessionService.terminateSession(sessionId);
        return ResponseEntity.ok(Map.of("message", "Session terminated successfully"));
    }

    @DeleteMapping("/sessions/all")
    public ResponseEntity<Map<String, String>> terminateAllSessions(@AuthenticationPrincipal UserPrincipal principal) {
        sessionService.invalidateAllSessionsOfUser(principal.getId());
        return ResponseEntity.ok(Map.of("message", "All active sessions terminated"));
    }

    @GetMapping("/my-devices")
    public ResponseEntity<List<DeviceResponse>> getMyDevices(@AuthenticationPrincipal UserPrincipal principal) {
        User user = userRepository.findById(principal.getId()).orElseThrow();
        return ResponseEntity.ok(selfServicePortalService.getMyDevices(user));
    }
}
