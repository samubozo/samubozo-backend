package com.playdata.certificateservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor // 기본 생성자 추가
@AllArgsConstructor // 선택 사항: 모든 필드를 인자로 받는 생성자 (빌더 패턴 등에서 유용)
public class ChatRequest {
    private Long employeeNo; // 사용자 식별자 (employeeNo로 변경)
    private String conversationId; // 대화 세션 식별자 추가
    private String message;
}