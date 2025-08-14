package com.playdata.chatbotservice.service;

import com.playdata.chatbotservice.dto.ChatRequest;
import com.playdata.chatbotservice.dto.ChatResponse;
import com.playdata.chatbotservice.dto.gemini.GeminiRequest;
import com.playdata.chatbotservice.dto.gemini.GeminiResponse;
import com.playdata.chatbotservice.entity.ChatMessage;

import java.util.List;

public interface ChatbotService {
    ChatResponse getChatResponse(ChatRequest request);

    // Gemini API 요청 본문을 생성하는 메서드
    GeminiRequest createGeminiRequestBody(Long employeeNo, String conversationId, String userMessage);

    // Gemini 응답에서 텍스트 내용을 추출하는 메서드
    String extractGeminiResponseContent(GeminiResponse response);

    void saveChatMessage(Long employeeNo, String conversationId, String messageContent, ChatMessage.SenderType senderType);

    List<ChatMessage> getChatHistory(Long employeeNo);

}
