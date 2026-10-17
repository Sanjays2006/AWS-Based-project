package com.cloudidentity.controller;

import com.cloudidentity.dto.response.AnomalyEventResponse;
import com.cloudidentity.dto.response.PagedResponse;
import com.cloudidentity.entity.AnomalyEvent;
import com.cloudidentity.entity.User;
import com.cloudidentity.exception.ResourceNotFoundException;
import com.cloudidentity.repository.AnomalyEventRepository;
import com.cloudidentity.repository.UserRepository;
import com.cloudidentity.security.UserPrincipal;
import com.cloudidentity.util.MapperUtil;
import com.cloudidentity.util.PaginationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/anomalies")
@RequiredArgsConstructor
public class AnomalyEventController {

    private final AnomalyEventRepository anomalyEventRepository;
    private final UserRepository userRepository;
    private final MapperUtil mapperUtil;

    @GetMapping
    public ResponseEntity<PagedResponse<AnomalyEventResponse>> listAnomalies(
            @RequestParam(required = false) Long identityId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) Boolean isAcknowledged,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<AnomalyEvent> eventPage = anomalyEventRepository.filterAnomalies(
                identityId, severity, isAcknowledged, startDate, endDate,
                PaginationUtil.createPageable(page, size, "createdAt", "DESC"));

        return ResponseEntity.ok(PaginationUtil.toPagedResponse(eventPage,
                eventPage.getContent().stream().map(mapperUtil::toAnomalyEventResponse).collect(Collectors.toList())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AnomalyEventResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(mapperUtil.toAnomalyEventResponse(
                anomalyEventRepository.findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Anomaly event not found: " + id))));
    }

    @PatchMapping("/{id}/acknowledge")
    public ResponseEntity<Map<String, String>> acknowledge(@PathVariable Long id,
                                                           @AuthenticationPrincipal UserPrincipal principal) {
        AnomalyEvent event = anomalyEventRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anomaly event not found: " + id));

        User user = userRepository.findById(principal.getId()).orElseThrow();
        event.setIsAcknowledged(true);
        event.setAcknowledgedByUser(user);
        event.setAcknowledgedAt(LocalDateTime.now());
        anomalyEventRepository.save(event);

        return ResponseEntity.ok(Map.of("message", "Anomaly event acknowledged successfully"));
    }
}
