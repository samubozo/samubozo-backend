package com.playdata.messageservice.service;

import com.playdata.messageservice.dto.NotificationResponse;
import com.playdata.messageservice.entity.Notification;
import com.playdata.messageservice.repository.NotificationRepository;
import com.playdata.messageservice.type.NotificationType; // NotificationType import 추가
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    private Map<String, SseEmitter> emitters; // NotificationService 내부의 emitters 맵에 접근하기 위함

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        // Reflection을 사용하여 private 필드인 emitters 맵에 접근
        java.lang.reflect.Field field = NotificationService.class.getDeclaredField("emitters");
        field.setAccessible(true);
        emitters = (Map<String, SseEmitter>) field.get(notificationService);
        emitters.clear(); // 각 테스트 전에 맵 초기화
    }

    @DisplayName("SSE 구독 테스트 - 새로운 구독")
    @Test
    void subscribe_newEmitter() {
        String employeeNo = "testUser1";

        SseEmitter emitter = notificationService.subscribe(employeeNo);

        assertNotNull(emitter);
        assertTrue(emitters.containsKey(employeeNo));
        assertThat(emitters.get(employeeNo)).isEqualTo(emitter);

        // SseEmitter의 send 메서드는 IOException을 던질 수 있으므로,
        // 실제 send 호출을 테스트하려면 SseEmitter를 Mocking해야 하지만,
        // 여기서는 서비스가 SseEmitter를 생성하므로 직접 Mocking하기 어렵습니다.
        // 대신, emitters 맵에 올바르게 추가되었는지 확인하는 것으로 충분합니다.
        // 초기 연결 이벤트 전송은 sendNotificationToClient와는 다른 경로이므로,
        // SseEmitter의 send 메서드를 스파이하거나, 더미 이벤트를 보내는 로직을
        // 별도의 private 메서드로 분리하여 테스트하는 것이 좋습니다.
        // 현재는 SseEmitter가 실제 객체이므로 verify는 불가능합니다.
    }

    @DisplayName("SSE 구독 테스트 - 기존 구독 대체")
    @Test
    void subscribe_replaceExistingEmitter() throws IOException {
        String employeeNo = "testUser1";
        SseEmitter oldEmitter = mock(SseEmitter.class); // 기존 Emitter는 Mock으로
        emitters.put(employeeNo, oldEmitter);

        SseEmitter newEmitter = notificationService.subscribe(employeeNo);

        assertNotNull(newEmitter);
        assertTrue(emitters.containsKey(employeeNo));
        assertThat(emitters.get(employeeNo)).isEqualTo(newEmitter);
        assertNotEquals(oldEmitter, newEmitter);

        // 기존 Emitter가 complete 되었는지 확인
        verify(oldEmitter, times(1)).complete();
        // 새로운 Emitter의 초기 연결 이벤트 전송은 위와 동일한 이유로 직접 verify하기 어렵습니다.
    }

    @DisplayName("알림 생성 및 DB 저장 테스트 - 온라인 사용자")
    @Test
    void createNotification_onlineUser_savesAndSendsSse() throws IOException {
        String employeeNo = "testUser1";
        NotificationType type = NotificationType.MESSAGE; // String -> NotificationType 변경
        String message = "새로운 메시지가 도착했습니다.";
        Long messageId = 1L;

        // save 메서드가 호출될 때 반환할 Notification 객체 (ID와 createdAt이 설정된 상태)
        Notification savedNotification = Notification.builder()
                .notificationId(1L)
                .employeeNo(employeeNo)
                .type(type)
                .message(message)
                .messageId(messageId)
                .isRead(false)
                .createdAt(LocalDateTime.now()) // 실제 DB 저장 시 생성될 값
                .build();

        // notificationRepository.save가 호출될 때 savedNotification을 반환하도록 Mocking
        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        // SSE Emitter가 존재하도록 설정 (Mock SseEmitter 사용)
        SseEmitter mockEmitter = mock(SseEmitter.class);
        emitters.put(employeeNo, mockEmitter);

        Notification result = notificationService.createNotification(employeeNo, type, message, messageId);

        assertNotNull(result);
        assertThat(result.getEmployeeNo()).isEqualTo(employeeNo);
        assertThat(result.getIsRead()).isFalse();
        assertThat(result.getNotificationId()).isEqualTo(1L); // 저장 후 ID가 설정되었는지 확인

        // DB에 저장되었는지 확인
        ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());
        Notification capturedNotification = notificationCaptor.getValue();
        assertThat(capturedNotification.getEmployeeNo()).isEqualTo(employeeNo);
        assertThat(capturedNotification.getIsRead()).isFalse();
        assertThat(capturedNotification.getType()).isEqualTo(type);
        assertThat(capturedNotification.getMessage()).isEqualTo(message);
        assertThat(capturedNotification.getMessageId()).isEqualTo(messageId);

        // SSE 이벤트가 전송되었는지 확인 (SseEmitter.SseEventBuilder.class 사용)
        verify(mockEmitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @DisplayName("알림 생성 및 DB 저장 테스트 - 오프라인 사용자")
    @Test
    void createNotification_offlineUser_savesOnlyToDb() throws IOException {
        String employeeNo = "testUser2";
        NotificationType type = NotificationType.MESSAGE; // String -> NotificationType 변경
        String message = "새로운 메시지가 도착했습니다.";
        Long messageId = 2L;

        Notification savedNotification = Notification.builder()
                .notificationId(2L)
                .employeeNo(employeeNo)
                .type(type)
                .message(message)
                .messageId(messageId)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepository.save(any(Notification.class))).thenReturn(savedNotification);

        // SSE Emitter가 존재하지 않도록 설정 (emitters 맵에 추가하지 않음)

        Notification result = notificationService.createNotification(employeeNo, type, message, messageId);

        assertNotNull(result);
        assertThat(result.getEmployeeNo()).isEqualTo(employeeNo);
        assertThat(result.getIsRead()).isFalse();
        assertThat(result.getNotificationId()).isEqualTo(2L);

        // DB에 저장되었는지 확인
        verify(notificationRepository, times(1)).save(any(Notification.class));

        // SSE 이벤트가 전송되지 않았는지 확인 (mockEmitter가 없으므로 verify할 필요 없음)
        // sendNotificationToClient 내부에서 emitter가 null일 때의 로직이 잘 동작하는지 간접적으로 확인
        // 즉, mockEmitter.send()가 호출되지 않았음을 암시적으로 확인
    }

    @DisplayName("읽지 않은 알림 개수 조회 테스트")
    @Test
    void getUnreadNotificationCount() {
        String employeeNo = "testUser3";
        long expectedCount = 5L;
        when(notificationRepository.countByEmployeeNoAndIsReadFalse(employeeNo)).thenReturn(expectedCount);

        long actualCount = notificationService.getUnreadNotificationCount(employeeNo);

        assertThat(actualCount).isEqualTo(expectedCount);
        verify(notificationRepository, times(1)).countByEmployeeNoAndIsReadFalse(employeeNo);
    }

    @DisplayName("알림 읽음 처리 테스트")
    @Test
    void markNotificationAsRead() {
        Long notificationId = 1L;
        String employeeNo = "testUser4";
        Notification notification = Notification.builder()
                .notificationId(notificationId)
                .employeeNo(employeeNo)
                .isRead(false)
                .build();

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        notificationService.markNotificationAsRead(notificationId);

        assertTrue(notification.getIsRead());
        assertNotNull(notification.getReadAt());
        verify(notificationRepository, times(1)).save(notification);
    }

    @DisplayName("알림 목록 조회 테스트")
    @Test
    void getNotifications() {
        String employeeNo = "testUser5";
        Notification noti1 = Notification.builder().notificationId(1L).employeeNo(employeeNo).message("noti1").createdAt(LocalDateTime.now().minusDays(1)).isRead(false).build();
        Notification noti2 = Notification.builder().notificationId(2L).employeeNo(employeeNo).message("noti2").createdAt(LocalDateTime.now()).isRead(false).build();

        when(notificationRepository.findByEmployeeNoOrderByCreatedAtDesc(employeeNo))
                .thenReturn(Arrays.asList(noti2, noti1)); // 최신순

        List<NotificationResponse> notifications = notificationService.getNotifications(employeeNo);

        assertThat(notifications).hasSize(2);
        assertThat(notifications.get(0).getMessage()).isEqualTo("noti2");
        assertThat(notifications.get(1).getMessage()).isEqualTo("noti1");
        verify(notificationRepository, times(1)).findByEmployeeNoOrderByCreatedAtDesc(employeeNo);
    }
}
