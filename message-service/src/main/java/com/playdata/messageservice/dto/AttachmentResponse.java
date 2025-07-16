package com.playdata.messageservice.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AttachmentResponse {
    private Long attachmentId;
    private String attachmentUrl;
    private String originalFileName;
}
