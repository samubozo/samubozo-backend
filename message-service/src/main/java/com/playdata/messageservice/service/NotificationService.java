package com.playdata.messageservice.service;

import com.playdata.messageservice.dto.NotificationResponse;
import com.playdata.messageservice.entity.Notification;
import com.playdata.messageservice.type.NotificationType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

public interface NotificationService {
    // SseEmitter를 등록하고 관리
    SseEmitter subscribe(String employeeNo);

    @Transactional
    void createNotification(String employeeNo, NotificationType type, String message, Long messageId);

    // 클라이언트에게 알림 전송
    void sendNotificationToClient(Notification notification);

    @Transactional
    NotificationResponse markNotificationAsRead(Long notificationId);

    @Transactional
    void markNotificationAsReadByMessageId(Long messageId);

    // 특정 사용자의 모든 알림 조회 (최신순)
    List<NotificationResponse> getNotifications(String employeeNo);

    // 특정 사용자의 읽지 않은 알림 개수 조회
    long getUnreadNotificationCount(String employeeNo);

    @Transactional
    void deleteNotificationsByMessageId(Long messageId);

    NotificationResponse convertToDto(Notification notification);
}
