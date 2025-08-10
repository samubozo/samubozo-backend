package com.playdata.notificationservice.controller;

import com.playdata.notificationservice.dto.NotificationResponse;
import com.playdata.notificationservice.service.NotificationService;
import com.playdata.notificationservice.dto.NotificationCreateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    // SSE 구독 엔드포인트
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestHeader("X-User-Employee-No") String employeeNo) {
        return notificationService.subscribe(employeeNo);
    }

    // 알림 생성 처리
    @PostMapping
    public ResponseEntity<NotificationResponse> createNotification(@RequestBody NotificationCreateRequest request) {
        NotificationResponse response = notificationService.createNotification(request.getEmployeeNo(), request.getType(), request.getMessage(), request.getMessageId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 알림 읽음 처리
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@PathVariable Long notificationId) {
        NotificationResponse response = notificationService.markNotificationAsRead(notificationId);
        return ResponseEntity.ok(response);
    }

    // 메세지를 알림보다 먼저 볼시 ID로 알림 읽음 처리
    @PostMapping("/mark-read-by-message-id")
    public ResponseEntity<NotificationResponse> markReadByMessageId(@RequestBody Long messageId) {
        NotificationResponse response = notificationService.markNotificationAsRead(messageId);
        return ResponseEntity.ok(response);
    }
    @PostMapping("/delete-by-message-id")
    public ResponseEntity<Void> deleteNotificationsByMessageId(@RequestBody Long messageId){
        notificationService.deleteNotificationsByMessageId(messageId);
        return ResponseEntity.ok().build();
    }

    // 알림 목록 조회
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @RequestHeader("X-User-Employee-No") String employeeNo) {
        List<NotificationResponse> notifications = notificationService.getNotifications(employeeNo);
        return ResponseEntity.ok(notifications);
    }

    // 읽지 않은 알림 개수 조회
    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadNotificationCount(
            @RequestHeader("X-User-Employee-No") String employeeNo) {
        long count = notificationService.getUnreadNotificationCount(employeeNo);
        return ResponseEntity.ok(count);
    }
}
