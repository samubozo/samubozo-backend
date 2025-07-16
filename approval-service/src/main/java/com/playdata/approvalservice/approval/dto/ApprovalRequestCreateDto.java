package com.playdata.approvalservice.approval.dto;

import com.playdata.approvalservice.approval.entity.RequestType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApprovalRequestCreateDto {
    private RequestType requestType;
    private Long applicantId;
    private String title; // 추가
    private String reason;
    private Long vacationsId; // Optional, for VACATION request type
    private String vacationType; // Optional, for VACATION request type
    private Long certificatesId; // Optional, for CERTIFICATE request type
}
