package com.playdata.chatbotservice.controller;

import com.playdata.chatbotservice.dto.ChatRequest;
import com.playdata.chatbotservice.dto.ChatResponse;
import com.playdata.chatbotservice.entity.ChatMessage;
import com.playdata.chatbotservice.service.ChatbotService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.playdata.chatbotservice.common.auth.TokenUserInfo;

import java.util.List;
import com.playdata.chatbotservice.config.BadWordsConfig;
import com.playdata.chatbotservice.config.BusinessKeywordsConfig;

@RestController
@RequestMapping("/chatbot")
public class ChatbotController {

    private final ChatbotService chatbotService;

    

    

    public ChatbotController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @GetMapping("/hello")
    public String helloChatbot() {
        return "Hello, Chatbot!";
    }

    @PostMapping("/chat")
    public Mono<ChatResponse> chatWithBot(@RequestBody ChatRequest request, @AuthenticationPrincipal TokenUserInfo tokenUserInfo) {
        String userMessage = request.getMessage();
        Long employeeNo = tokenUserInfo.getEmployeeNo(); // 토큰에서 employeeNo 추출

        // 1. 욕설 필터링 (블랙리스트)
        for (String badWord : BadWordsConfig.BAD_WORDS) {
            if (userMessage.toLowerCase().replace(" ", "").contains(badWord.toLowerCase().replace(" ", ""))) {
                String conversationId = request.getConversationId() != null && !request.getConversationId().trim().isEmpty() ?
                                        request.getConversationId() : java.util.UUID.randomUUID().toString();
                return Mono.just(new ChatResponse("부적절한 언어 사용은 삼가주세요.", conversationId));
            }
        }

        // 2. 업무 관련 키워드 필터링 (화이트리스트)
        boolean isBusinessRelated = false;
        for (String keyword : BusinessKeywordsConfig.BUSINESS_KEYWORDS) {
            if (userMessage.toLowerCase().replace(" ", "").contains(keyword.toLowerCase().replace(" ", ""))) {
                isBusinessRelated = true;
                break;
            }
        }

        if (!isBusinessRelated) {
            String conversationId = request.getConversationId() != null && !request.getConversationId().trim().isEmpty() ?
                                    request.getConversationId() : java.util.UUID.randomUUID().toString();
            return Mono.just(new ChatResponse("저는 회사 규정 및 복지에 대한 질문에만 답변할 수 있습니다. 다른 질문은 도와드릴 수 없습니다.", conversationId));
        }

        // ChatRequest에 employeeNo 설정
        request.setEmployeeNo(employeeNo);
        return chatbotService.getChatResponse(request);
    }

    @GetMapping("/history") // 경로 변경
    public List<ChatMessage> getChatHistory(@AuthenticationPrincipal TokenUserInfo tokenUserInfo) {
        Long employeeNo = tokenUserInfo.getEmployeeNo(); // 토큰에서 employeeNo 추출
        return chatbotService.getChatHistory(employeeNo);
    }
}
