package com.playdata.chatbotservice.repository;

import com.playdata.chatbotservice.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByUserIdOrderByTimestampAsc(String userId);

    // 추가: userId와 conversationId로 메시지 조회
    List<ChatMessage> findByUserIdAndConversationIdOrderByTimestampAsc(String userId, String conversationId);
}
