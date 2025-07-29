package com.playdata.notificationservice.service;

import com.playdata.notificationservice.dto.NotificationResponse;
import com.playdata.notificationservice.entity.Notification;
import com.playdata.notificationservice.repository.NotificationRepository;
import com.playdata.notificationservice.type.NotificationType; // NotificationType import 추가
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    // 사용자별 SseEmitter를 저장하는 맵
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    // SseEmitter를 등록하고 관리
    @Override
    public SseEmitter subscribe(String employeeNo) {
        // 기존 Emitter가 있다면 종료
        if (emitters.containsKey(employeeNo)) {
            emitters.get(employeeNo).complete();
            emitters.remove(employeeNo);
        }

        SseEmitter emitter = new SseEmitter(60 * 60 * 1000L); // 1시간 타임아웃
        emitters.put(employeeNo, emitter);

        // 연결이 끊기거나 타임아웃될 때 Emitter 제거
        emitter.onCompletion(() -> emitters.remove(employeeNo));
        emitter.onTimeout(() -> {
            log.warn("SSE Emitter timeout for employeeNo: {}", employeeNo);
            emitter.complete();
            emitters.remove(employeeNo);
        });
        emitter.onError(e -> {
            log.error("SSE Emitter error for employeeNo: {}", employeeNo, e);
            emitter.complete();
            emitters.remove(employeeNo);
        });

        // 503 Service Unavailable 방지를 위한 더미 이벤트 전송
        try {
            emitter.send(SseEmitter.event().name("connect").data("connected!"));
        } catch (IOException e) {
            log.error("SSE connect event send failed for employeeNo: {}", employeeNo, e);
        }

        log.info("SSE Emitter subscribed for employeeNo: {}", employeeNo);
        return emitter;
    }

    @Transactional
    @Override
    public NotificationResponse createNotification(String employeeNo, NotificationType type, String message, Long messageId) { // String -> NotificationType 변경
        // 1. 알림을 데이터베이스에 저장
        Notification notification = Notification.builder()
                .employeeNo(employeeNo)
                .type(type) // NotificationType 사용
                .message(message)
                .messageId(messageId)
                .isRead(false) // 처음 생성 시 읽지 않은 상태
                .build();

        Notification savedNotification = notificationRepository.save(notification);
        log.info("Notification saved to DB for employeeNo {}: {}", employeeNo, message);

        // 2. 알림 생성 후 해당 사용자에게 SSE 이벤트 전송 (온라인 사용자만)
        sendNotificationToClient(savedNotification);
        return convertToDto(savedNotification);
    }

    // 클라이언트에게 알림 전송
    @Override
    public void sendNotificationToClient(Notification notification) {
        SseEmitter emitter = emitters.get(notification.getEmployeeNo());
        if (emitter != null) {
            try {
                NotificationResponse response = convertToDto(notification);
                emitter.send(SseEmitter.event().name("notification").data(response));
                log.info("Notification sent to employeeNo {}: {}", notification.getEmployeeNo(), notification.getMessage());
            } catch (IOException e) {
                log.error("Failed to send notification to employeeNo {}: {}", notification.getEmployeeNo(), e.getMessage());
                emitters.remove(notification.getEmployeeNo()); // 전송 실패 시 Emitter 제거
            }
        } else {
            // 사용자가 현재 접속 중이 아님 (SSE 연결 없음), DB에만 저장된 상태로 유지
            log.warn("No active SSE Emitter for employeeNo: {}. Notification will remain in DB.", notification.getEmployeeNo());
        }
    }

    @Transactional
    @Override
    public NotificationResponse markNotificationAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found with id: " + notificationId));

        if (!notification.getIsRead()) {
            notification.setIsRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
            log.info("Notification {} marked as read for employeeNo {}", notificationId, notification.getEmployeeNo());
        }
        return convertToDto(notification);
    }

    @Transactional
    @Override
    public void markNotificationAsReadByMessageId(Long messageId) {
        List<Notification> notifications = notificationRepository.findByMessageId(messageId);
        for (Notification notification : notifications) {
            if (!notification.getIsRead()) {
                notification.setIsRead(true);
                notification.setReadAt(LocalDateTime.now());
                notificationRepository.save(notification);
                log.info("Notification {} (related to messageId {}) marked as read for employeeNo {}", notification.getNotificationId(), messageId, notification.getEmployeeNo());
            }
        }
    }

    // 특정 사용자의 모든 알림 조회 (최신순)
    @Override
    public List<NotificationResponse> getNotifications(String employeeNo) {
        List<Notification> notifications = notificationRepository.findByEmployeeNoOrderByCreatedAtDesc(employeeNo);
        return notifications.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // 특정 사용자의 읽지 않은 알림 개수 조회
    @Override
    public long getUnreadNotificationCount(String employeeNo) {
        long count = notificationRepository.countByEmployeeNoAndIsReadFalse(employeeNo);
        log.info("Unread notification count for employeeNo {}: {}", employeeNo, count);
        return count;
    }

    @Transactional
    @Override
    public void deleteNotificationsByMessageId(Long messageId) {
        List<Notification> notifications = notificationRepository.findByMessageId(messageId);
        if (!notifications.isEmpty()) {
            notificationRepository.deleteAll(notifications);
            log.info("Deleted {} notifications related to messageId: {}", notifications.size(), messageId);
        }
    }

    @Override
    public NotificationResponse convertToDto(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .employeeNo(notification.getEmployeeNo())
                .type(notification.getType()) // NotificationType 사용
                .message(notification.getMessage())
                .messageId(notification.getMessageId())
                .createdAt(notification.getCreatedAt())
                .isRead(notification.getIsRead())
                .readAt(notification.getReadAt())
                .build();
    }
}
