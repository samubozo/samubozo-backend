package com.playdata.vacationservice.client.dto;

import com.playdata.vacationservice.vacation.entity.RequestType;
import com.playdata.vacationservice.vacation.entity.VacationType;
import com.playdata.vacationservice.vacation.entity.VacationType;
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

    private RequestType requestType;
    private Long applicantId;
    private String title; // 추가
    private String reason;
    private Long vacationsId;
    private String vacationType;
    private Long certificatesId;
}