package com.playdata.messageservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class MessageResponse {
    private Long messageId;
    private Long senderId;
    private String senderName;
    private String senderDepartmentName;
    private Long receiverId;
    private String receiverName;
    private String subject;
    private String content;
    private LocalDateTime sentAt;
    private Boolean isRead;
    private LocalDateTime readAt;
    private Boolean isNotice;
    private List<AttachmentResponse> attachments;
}