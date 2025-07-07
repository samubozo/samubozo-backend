package com.playdata.approvalservice.approval.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApprovalRequestDto {
    private String approvalType;
    private Long userId;
    private String title;
    private String content;
    private Long referenceId;
}
