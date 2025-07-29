package com.playdata.messageservice.client;

import com.playdata.messageservice.common.configs.FeignClientConfiguration;
import com.playdata.messageservice.type.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "notification-service", configuration = FeignClientConfiguration.class)
public interface NotificationServiceClient {

    @PostMapping("/notifications")
    void createNotification(
            @RequestHeader("X-User-Employee-No") String employeeNo,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Role") String userRole,
            @RequestBody NotificationCreateRequest request);

    @PostMapping("/notifications/mark-read-by-message-id")
    void markNotificationAsReadByMessageId(
            @RequestHeader("X-User-Employee-No") String employeeNo,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Role") String userRole,
            @RequestBody Long messageId);

    @PostMapping("/notifications/delete-by-message-id")
    void deleteNotificationsByMessageId(
            @RequestHeader("X-User-Employee-No") String employeeNo,
            @RequestHeader("X-User-Email") String userEmail,
            @RequestHeader("X-User-Role") String userRole,
            @RequestBody Long messageId);

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    class NotificationCreateRequest {
        private String employeeNo;
        private NotificationType type;
        private String message;
        private Long messageId;
    }
}