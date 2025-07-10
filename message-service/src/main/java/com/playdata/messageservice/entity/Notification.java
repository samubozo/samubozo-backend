package com.playdata.messageservice.entity;

import com.playdata.messageservice.type.NotificationType; // NotificationType import 추가
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "tbl_notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "employee_no", nullable = false, length = 36)
    private String employeeNo;

    @Enumerated(EnumType.STRING) // EnumType.STRING 추가
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type; // String -> NotificationType 변경

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "message_id")
    private Long messageId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "read_at")
    private LocalDateTime readAt;
}
