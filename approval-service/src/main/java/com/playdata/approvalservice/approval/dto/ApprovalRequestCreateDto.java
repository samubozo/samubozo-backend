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
    private Long certificateId; // Optional, for CERTIFICATE request type
    private String certificateType; // 추가: CERTIFICATE 요청 타입의 구체적인 유형 (재직, 경력 등)
    private java.time.LocalDate startDate; // 추가
    private java.time.LocalDate endDate; // 추가
}
