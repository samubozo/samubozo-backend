package com.playdata.chatbotservice.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId; // 사용자 식별자

    @Column(nullable = false, columnDefinition = "TEXT")
    private String messageContent; // 메시지 내용

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SenderType senderType; // 발신자 유형 (USER, BOT)

    @Column(nullable = false)
    private LocalDateTime timestamp; // 메시지 전송 시간

    public enum SenderType {
        USER,
        BOT
    }
}
