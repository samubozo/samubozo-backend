package com.playdata.messageservice.dto;

import lombok.Getter;
import lombok.Setter;

import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class MessageRequest {
    private Long receiverId;
    private String subject;
    private String content;
    private MultipartFile attachment;
}