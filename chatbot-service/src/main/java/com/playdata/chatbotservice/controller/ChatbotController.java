package com.playdata.chatbotservice.controller;

import com.playdata.chatbotservice.dto.ChatRequest;
import com.playdata.chatbotservice.dto.ChatResponse;
import com.playdata.chatbotservice.entity.ChatMessage;
import com.playdata.chatbotservice.service.ChatbotService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.playdata.chatbotservice.common.auth.TokenUserInfo;

import java.util.List;

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
    public ResponseEntity<ChatResponse> chatWithBot(@RequestBody ChatRequest request, @AuthenticationPrincipal TokenUserInfo tokenUserInfo) {
        Long employeeNo = tokenUserInfo.getEmployeeNo(); // 토큰에서 employeeNo 추출

        // ChatRequest에 employeeNo 설정
        request.setEmployeeNo(employeeNo);
        ChatResponse response = chatbotService.getChatResponse(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history") // 경로 변경
    public List<ChatMessage> getChatHistory(@AuthenticationPrincipal TokenUserInfo tokenUserInfo) {
        Long employeeNo = tokenUserInfo.getEmployeeNo(); // 토큰에서 employeeNo 추출
        return chatbotService.getChatHistory(employeeNo);
    }
}
