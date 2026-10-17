package com.cloudidentity.controller;

import com.cloudidentity.dto.response.ActivityLogResponse;
import com.cloudidentity.dto.response.PagedResponse;
import com.cloudidentity.service.ActivityMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/activity-logs")
@RequiredArgsConstructor
public class ActivityLogController {

    private final ActivityMonitorService activityMonitorService;

    @GetMapping
    public ResponseEntity<PagedResponse<ActivityLogResponse>> listLogs(
            @RequestParam(required = false) Long identityId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "eventTime") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {
        return ResponseEntity.ok(activityMonitorService.listLogs(identityId, search, startDate, endDate, page, size, sortBy, sortDir));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ActivityLogResponse> getLogById(@PathVariable Long id) {
        return ResponseEntity.ok(activityMonitorService.getLogById(id));
    }
}
