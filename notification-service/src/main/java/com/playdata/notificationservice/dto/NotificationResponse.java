package com.playdata.notificationservice.dto;

import com.playdata.notificationservice.type.NotificationType; // NotificationType import 추가
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class NotificationResponse {
    private Long notificationId;
    private String employeeNo;
    private NotificationType type; // String -> NotificationType 변경
    private String message;
    private Long messageId;
    private LocalDateTime createdAt;
    private Boolean isRead;
    private LocalDateTime readAt;
}
