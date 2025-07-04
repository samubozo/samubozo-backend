package com.playdata.chatbotservice.service;

import com.playdata.chatbotservice.dto.ChatRequest;
import com.playdata.chatbotservice.dto.ChatResponse;
import com.playdata.chatbotservice.dto.gemini.Content;
import com.playdata.chatbotservice.dto.gemini.GeminiRequest;
import com.playdata.chatbotservice.dto.gemini.GeminiResponse;
import com.playdata.chatbotservice.dto.gemini.Part;
import com.playdata.chatbotservice.entity.ChatMessage;
import com.playdata.chatbotservice.entity.ChatMessage.SenderType;
import com.playdata.chatbotservice.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class ChatbotService {

    @Value("${ai.api.key}")
    private String aiApiKey;

    @Value("${ai.api.url}")
    private String aiApiUrl;

    private final WebClient webClient;
    private final ChatMessageRepository chatMessageRepository;

    public ChatbotService(WebClient.Builder webClientBuilder, ChatMessageRepository chatMessageRepository) {
        this.webClient = webClientBuilder.baseUrl(aiApiUrl).build();
        this.chatMessageRepository = chatMessageRepository;
    }

    public Mono<ChatResponse> getChatResponse(ChatRequest request) {
        // 사용자 메시지 저장
        saveChatMessage(request.getUserId(), request.getMessage(), SenderType.USER);

        // Gemini API 요청 본문 생성
        GeminiRequest geminiRequest = createGeminiRequestBody(request.getMessage());

        // Gemini API 호출
        Mono<String> aiResponseMono = webClient.post()
                .uri(uriBuilder -> uriBuilder.path("").queryParam("key", aiApiKey).build())
                .bodyValue(geminiRequest)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .map(this::extractGeminiResponseContent) // 응답에서 텍스트 내용 추출
                .onErrorResume(e -> {
                    // AI API 호출 실패 시 대체 메시지 반환
                    System.err.println("AI API 호출 실패: " + e.getMessage());
                    return Mono.just("챗봇이 휴가라서 지금 답변이 어려워요. 잠시후에 연락해주세요");
                });

        return aiResponseMono.flatMap(botResponseContent -> {
            ChatResponse botResponse = new ChatResponse(botResponseContent);
            // 챗봇 응답 저장
            saveChatMessage(request.getUserId(), botResponseContent, SenderType.BOT);
            return Mono.just(botResponse);
        });
    }

    // Gemini API 요청 본문을 생성하는 메서드
    private GeminiRequest createGeminiRequestBody(String userMessage) {
        Part part = new Part(userMessage);
        Content content = new Content(Collections.singletonList(part));
        return new GeminiRequest(Collections.singletonList(content));
    }

    // Gemini 응답에서 텍스트 내용을 추출하는 메서드
    private String extractGeminiResponseContent(GeminiResponse response) {
        if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
            Content content = response.getCandidates().get(0).getContent();
            if (content != null && content.getParts() != null && !content.getParts().isEmpty()) {
                return content.getParts().get(0).getText();
            }
        }
        return "응답을 파싱할 수 없습니다."; // 응답 파싱 실패 시 기본 메시지
    }

    private void saveChatMessage(String userId, String messageContent, SenderType senderType) {
        ChatMessage chatMessage = new ChatMessage(
                null, // ID는 자동 생성
                userId,
                messageContent,
                senderType,
                LocalDateTime.now()
        );
        chatMessageRepository.save(chatMessage);
    }

    public List<ChatMessage> getChatHistory(String userId) {
        return chatMessageRepository.findByUserIdOrderByTimestampAsc(userId);
    }
}