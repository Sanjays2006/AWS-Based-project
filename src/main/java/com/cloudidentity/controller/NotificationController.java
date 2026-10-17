package com.cloudidentity.controller;

import com.cloudidentity.dto.response.AlertNotificationResponse;
import com.cloudidentity.dto.response.PagedResponse;
import com.cloudidentity.security.UserPrincipal;
import com.cloudidentity.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping("/unread")
    public ResponseEntity<List<AlertNotificationResponse>> getUnread(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getUnreadNotifications(principal.getId()));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(Map.of("count", notificationService.getUnreadCount(principal.getId())));
    }

    @GetMapping
    public ResponseEntity<PagedResponse<AlertNotificationResponse>> listAll(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationService.listNotifications(principal.getId(), page, size));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Map<String, String>> markAsRead(@PathVariable Long id,
                                                          @AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAsRead(id, principal.getId());
        return ResponseEntity.ok(Map.of("message", "Notification marked as read"));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, String>> markAllAsRead(@AuthenticationPrincipal UserPrincipal principal) {
        notificationService.markAllAsRead(principal.getId());
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
    }
}
