package com.playdata.messageservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class MessageResponse {
    private Long messageId;
    private Long senderId;
    private Long receiverId;
    private String subject;
    private String content;
    private LocalDateTime sentAt;
    private Boolean isRead;
    private LocalDateTime readAt;
    private String attachmentUrl;
}