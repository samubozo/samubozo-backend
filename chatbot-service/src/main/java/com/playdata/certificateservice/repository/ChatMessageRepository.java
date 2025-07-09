package com.playdata.certificateservice.repository;

import com.playdata.certificateservice.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByEmployeeNoOrderByTimestampAsc(Long employeeNo);

    // 추가: employeeNo와 conversationId로 메시지 조회
    List<ChatMessage> findByEmployeeNoAndConversationIdOrderByTimestampAsc(Long employeeNo, String conversationId);
}
