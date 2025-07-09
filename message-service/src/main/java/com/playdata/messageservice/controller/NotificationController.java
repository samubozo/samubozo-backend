package com.playdata.messageservice.controller;

import com.playdata.messageservice.dto.NotificationResponse;
import com.playdata.messageservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import com.playdata.messageservice.common.auth.TokenUserInfo;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // SSE 구독 엔드포인트
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@AuthenticationPrincipal TokenUserInfo tokenUserInfo) {
        String employeeNo = String.valueOf(tokenUserInfo.getEmployeeNo());
        return notificationService.subscribe(employeeNo);
    }

    // 알림 읽음 처리
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long notificationId) {
        NotificationResponse response = notificationService.markNotificationAsRead(notificationId);
        return ResponseEntity.ok(response);
    }

    // 알림 목록 조회
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo) {
        String employeeNo = String.valueOf(tokenUserInfo.getEmployeeNo());
        List<NotificationResponse> notifications = notificationService.getNotifications(employeeNo);
        return ResponseEntity.ok(notifications);
    }

    // 읽지 않은 알림 개수 조회
    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadNotificationCount(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo) {
        String employeeNo = String.valueOf(tokenUserInfo.getEmployeeNo());
        long count = notificationService.getUnreadNotificationCount(employeeNo);
        return ResponseEntity.ok(count);
    }
}