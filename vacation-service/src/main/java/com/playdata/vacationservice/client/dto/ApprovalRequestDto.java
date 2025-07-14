package com.playdata.vacationservice.client.dto;

import com.playdata.vacationservice.vacation.entity.RequestType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 결재 서비스에 결재 생성을 요청하기 위한 DTO 입니다.
 */
@Getter
@Builder
@AllArgsConstructor
public class ApprovalRequestDto {

    private RequestType requestType; // 결재 종류 (예: "VACATION")
    private Long applicantId;
    private String reason; // 결재 사유
    private Long vacationsId; // Optional, for VACATION request type
    private Long certificatesId; // Optional, for CERTIFICATE request type
}