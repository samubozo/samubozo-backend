package com.playdata.approvalservice.approval.dto;

import com.playdata.approvalservice.approval.entity.RequestType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApprovalRequestDto {
    private RequestType requestType;
    private Long applicantId;
    private String title;
    private String content;
    private Long referenceId;
}
