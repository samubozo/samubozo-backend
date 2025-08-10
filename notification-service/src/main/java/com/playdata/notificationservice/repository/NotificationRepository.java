package com.playdata.notificationservice.repository;

import com.playdata.notificationservice.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    // 특정 사용자(employeeNo)의 알림 목록 조회 (최신순)
    List<Notification> findByEmployeeNoOrderByCreatedAtDesc(String employeeNo);

    // 특정 사용자(employeeNo)의 읽지 않은 알림 개수 조회
    long countByEmployeeNoAndIsReadFalse(String employeeNo);

    // 특정 쪽지 ID와 관련된 알림 목록 조회
    List<Notification> findByMessageId(Long messageId);

    // 특정 쪽지 ID로 알림 읽음 처리
    Optional<Notification> findOneByMessageId(Long messageId);

    // 특정 시간 이전에 생성된 알림 삭제
    void deleteByCreatedAtBefore(java.time.LocalDateTime dateTime);
}