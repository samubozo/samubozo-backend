package com.playdata.chatbotservice.controller;

import com.playdata.chatbotservice.dto.ChatRequest;
import com.playdata.chatbotservice.dto.ChatResponse;
import com.playdata.chatbotservice.entity.ChatMessage;
import com.playdata.chatbotservice.service.ChatbotService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

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
    public Mono<ChatResponse> chatWithBot(@RequestBody ChatRequest request) {
        return chatbotService.getChatResponse(request);
    }

    @GetMapping("/history/{userId}")
    public List<ChatMessage> getChatHistory(@PathVariable String userId) {
        return chatbotService.getChatHistory(userId);
    }
}
