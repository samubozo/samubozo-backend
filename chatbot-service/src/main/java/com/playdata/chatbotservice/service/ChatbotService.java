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
import java.util.UUID;

@Service
public class ChatbotService {

    @Value("${ai.api.key}")
    private String aiApiKey;

    // aiApiUrl 필드는 WebClientConfig에서 사용하므로 여기서는 필요 없음 (혹은 단순히 정보용으로 유지)
    // @Value("${ai.api.url}")
    // private String aiApiUrl;

    private final WebClient webClient; // @Bean으로 등록한 WebClient를 주입받음
    private final ChatMessageRepository chatMessageRepository;

    // WebClientConfig에서 정의한 WebClient 빈을 주입받도록 생성자 수정
    public ChatbotService(WebClient webClient, ChatMessageRepository chatMessageRepository) {
        this.webClient = webClient; // 이미 baseUrl이 설정된 WebClient가 주입됨
        this.chatMessageRepository = chatMessageRepository;
    }

    public Mono<ChatResponse> getChatResponse(ChatRequest request) {
        String initialConversationId = request.getConversationId();
        final String effectiveConversationId;

        if (initialConversationId == null || initialConversationId.trim().isEmpty()) {
            effectiveConversationId = UUID.randomUUID().toString();
            request.setConversationId(effectiveConversationId); // request 객체에도 업데이트
        } else {
            effectiveConversationId = initialConversationId;
        }

        // 사용자 메시지 저장
        saveChatMessage(request.getEmployeeNo(), effectiveConversationId, request.getMessage(), SenderType.USER);

        // Gemini API 요청 본문 생성
        GeminiRequest geminiRequest = createGeminiRequestBody(request.getEmployeeNo(), effectiveConversationId, request.getMessage());

        // Gemini API 호출
        Mono<String> aiResponseMono = webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("") // baseUrl에 이미 전체 URL이 있으므로 path는 비워둠
                        .queryParam("key", aiApiKey) // API 키를 쿼리 파라미터로 추가
                        .build()
                )
                .bodyValue(geminiRequest)
                .retrieve()
                .bodyToMono(GeminiResponse.class)
                .map(this::extractGeminiResponseContent) // 응답에서 텍스트 내용 추출
                .onErrorResume(e -> {
                    System.err.println("AI API 호출 실패: " + e.getMessage());
                    if (e instanceof org.springframework.web.reactive.function.client.WebClientResponseException) {
                        org.springframework.web.reactive.function.client.WebClientResponseException wcRe = (org.springframework.web.reactive.function.client.WebClientResponseException) e;
                        System.err.println("AI API 응답 상태 코드: " + wcRe.getStatusCode());
                        System.err.println("AI API 응답 본문: " + wcRe.getResponseBodyAsString());
                    }
                    return Mono.just("챗봇이 휴가라서 지금 답변이 어려워요. 잠시후에 연락해주세요");
                });

        return aiResponseMono.flatMap(botResponseContent -> {
            ChatResponse botResponse = new ChatResponse(botResponseContent, effectiveConversationId);
            // 챗봇 응답 저장
            saveChatMessage(request.getEmployeeNo(), effectiveConversationId, botResponseContent, SenderType.BOT);
            return Mono.just(botResponse);
        });
    }

    // Gemini API 요청 본문을 생성하는 메서드
    private GeminiRequest createGeminiRequestBody(Long employeeNo, String conversationId, String userMessage) {
        // 챗봇의 역할과 답변 범위 제한 (프롬프트 엔지니어링)
        String systemInstruction =
                "당신은 회사의 AI 업무 도우미 챗봇입니다. 사용자의 질문이 회사 업무, 직무 수행, 사내 정책, 업무 도구, 조직문화, 커뮤니케이션, 일정/회의, 성과 향상 등과 관련 있다고 판단되면 간결하고 핵심적으로 답변합니다.\n" +
                        "응답은 3~5줄 이내로 요점을 중심으로 구성하며, 불필요한 설명은 피합니다.\n" +
                        "단, 명백히 개인적인 질문(예: 요리, 연예인, 스포츠, 게임 등)은 다음과 같이 응답합니다: '저는 업무 관련 질문에 답변할 수 있습니다.'\n" +
                        "경계가 모호한 경우에는 업무와 연결 가능한 방향으로 간단히 유도해도 좋습니다.\n\n";

        // 이전 대화 기록 조회
        List<ChatMessage> chatHistory = chatMessageRepository.findByEmployeeNoAndConversationIdOrderByTimestampAsc(employeeNo, conversationId);

        StringBuilder conversationContext = new StringBuilder();
        for (ChatMessage message : chatHistory) {
            String prefix = message.getSenderType() == SenderType.USER ? "사용자: " : "챗봇: ";
            conversationContext.append(prefix).append(message.getMessageContent()).append("\n");
        }

        // 시스템 지시, 대화 기록, 현재 사용자 메시지를 모두 결합
        String fullMessage = systemInstruction + "--- 이전 대화 ---\n" + conversationContext.toString() + "--- 현재 질문 ---\n" + "사용자: " + userMessage;


        Part part = new Part(fullMessage);
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

    private void saveChatMessage(Long employeeNo, String conversationId, String messageContent, SenderType senderType) {
        ChatMessage chatMessage = new ChatMessage(
                null, // ID는 자동 생성
                employeeNo,
                conversationId,
                messageContent,
                senderType,
                LocalDateTime.now()
        );
        chatMessageRepository.save(chatMessage);
    }

    public List<ChatMessage> getChatHistory(Long employeeNo) {
        return chatMessageRepository.findByEmployeeNoOrderByTimestampAsc(employeeNo);
    }

    // 추가: 특정 대화의 채팅 기록 조회
    public List<ChatMessage> getChatHistory(Long employeeNo, String conversationId) {
        return chatMessageRepository.findByEmployeeNoAndConversationIdOrderByTimestampAsc(employeeNo, conversationId);
    }
}