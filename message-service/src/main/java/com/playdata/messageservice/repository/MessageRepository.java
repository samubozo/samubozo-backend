package com.playdata.messageservice.repository;

import com.playdata.messageservice.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long>, MessageRepositoryCustom {
    // 특정 수신자의 쪽지 목록 조회 (최신순)
    List<Message> findByReceiverIdOrderBySentAtDesc(Long receiverId);

    // 특정 발신자가 보낸 쪽지 목록 조회 (최신순)
    List<Message> findBySenderIdOrderBySentAtDesc(Long senderId);

    // 특정 수신자의 읽지 않은 쪽지 개수 조회
    long countByReceiverIdAndIsReadFalse(Long receiverId);
}