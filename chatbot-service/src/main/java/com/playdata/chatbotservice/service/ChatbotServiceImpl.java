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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
public class ChatbotServiceImpl implements ChatbotService {

    @Value("${ai.api.key}")
    private String aiApiKey;

    @Value("${ai.api.url}")
    private String aiApiUrl;

    private final WebClient geminiWebClient; // WebClient 주입
    private final ChatMessageRepository chatMessageRepository;

    public ChatbotServiceImpl(WebClient geminiWebClient, ChatMessageRepository chatMessageRepository) {
        this.geminiWebClient = geminiWebClient;
        this.chatMessageRepository = chatMessageRepository;
    }

    @Override
    public ChatResponse getChatResponse(ChatRequest request) {
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

        String botResponseContent;
        try {
            // Gemini API 호출 (비동기식)
            GeminiResponse geminiResponse = geminiWebClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/").queryParam("key", aiApiKey).build())
                    .bodyValue(geminiRequest)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .block(); // 동기적으로 처리하기 위해 block() 사용
            botResponseContent = extractGeminiResponseContent(geminiResponse);
        } catch (HttpClientErrorException e) {
            System.err.println("AI API 호출 실패: " + e.getMessage());
            System.err.println("AI API 응답 상태 코드: " + e.getStatusCode());
            System.err.println("AI API 응답 본문: " + e.getResponseBodyAsString());
            botResponseContent = "챗봇이 휴가라서 지금 답변이 어려워요. 잠시후에 연락해주세요";
        } catch (Exception e) {
            System.err.println("AI API 호출 중 예상치 못한 오류 발생: " + e.getMessage());
            botResponseContent = "챗봇이 휴가라서 지금 답변이 어려워요. 잠시후에 연락해주세요";
        }

        ChatResponse botResponse = new ChatResponse(botResponseContent, effectiveConversationId);
        // 챗봇 응답 저장
        saveChatMessage(request.getEmployeeNo(), effectiveConversationId, botResponseContent, SenderType.BOT);
        return botResponse;
    }

    // Gemini API 요청 본문을 생성하는 메서드
    @Override
    public GeminiRequest createGeminiRequestBody(Long employeeNo, String conversationId, String userMessage) {
        // 챗봇의 역할과 답변 범위 제한 (프롬프트 엔지니어링)
        String systemInstruction =
                """
                        당신은 회사의 AI 업무 도우미 챗봇입니다. 사용자의 질문이 회사 업무, 직무 수행, 사내 정책, 업무 도구, 조직문화, 커뮤니케이션, 일정/회의, 성과 향상 등과 관련 있다고 판단되면 간결하고 핵심적으로 답변합니다.
                        응답은 3~5줄 이내로 요점을 중심으로 구성하며, 불필요한 설명은 피합니다.
                        단, 명백히 개인적인 질문(예: 요리, 연예인, 스포츠, 게임 등)은 다음과 같이 응답합니다: '저는 업무 관련 질문에 답변할 수 있습니다.'
                        경계가 모호한 경우에는 업무와 연결 가능한 방향으로 간단히 유도해도 좋습니다.
                        
                        """;

        // 이전 대화 기록 조회
        List<ChatMessage> chatHistory = chatMessageRepository.findByEmployeeNoAndConversationIdOrderByTimestampAsc(employeeNo, conversationId);

        StringBuilder conversationContext = new StringBuilder();
        for (ChatMessage message : chatHistory) {
            String prefix = message.getSenderType() == SenderType.USER ? "사용자: " : "챗봇: ";
            conversationContext.append(prefix).append(message.getMessageContent()).append("\n");
        }

        // 시스템 지시, 대화 기록, 현재 사용자 메시지를 모두 결합
        String fullMessage = systemInstruction + "--- 이전 대화 ---\n" + conversationContext + "--- 현재 질문 ---\n" + "사용자: " + userMessage;


        Part part = new Part(fullMessage);
        Content content = new Content(Collections.singletonList(part));
        return new GeminiRequest(Collections.singletonList(content));
    }

    // Gemini 응답에서 텍스트 내용을 추출하는 메서드
    @Override
    public String extractGeminiResponseContent(GeminiResponse response) {
        if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
            Content content = response.getCandidates().get(0).getContent();
            if (content != null && content.getParts() != null && !content.getParts().isEmpty()) {
                return content.getParts().get(0).getText();
            }
        }
        return "응답을 파싱할 수 없습니다."; // 응답 파싱 실패 시 기본 메시지
    }

    @Override
    public void saveChatMessage(Long employeeNo, String conversationId, String messageContent, SenderType senderType) {
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

    @Override
    public List<ChatMessage> getChatHistory(Long employeeNo) {
        return chatMessageRepository.findByEmployeeNoOrderByTimestampAsc(employeeNo);
    }
    
}