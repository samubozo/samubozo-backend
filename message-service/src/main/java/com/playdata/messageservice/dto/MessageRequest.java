package com.playdata.messageservice.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class MessageRequest {
    private List<Long> receiverIds;
    private String subject;
    private String content;
    private MultipartFile[] attachments;
    private Boolean isNotice;
}