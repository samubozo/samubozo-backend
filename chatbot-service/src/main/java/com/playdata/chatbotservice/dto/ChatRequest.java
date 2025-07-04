package com.playdata.chatbotservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChatRequest {
    private String userId; // 사용자 식별자 추가
    private String message;
}