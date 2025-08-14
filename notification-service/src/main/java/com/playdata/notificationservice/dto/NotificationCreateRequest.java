package com.playdata.notificationservice.dto;

import com.playdata.notificationservice.type.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCreateRequest {
    private String employeeNo;
    private NotificationType type;
    private String message;
    private Long messageId;
}
