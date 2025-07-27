package com.playdata.vacationservice.vacation.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 휴가 신청 승인/반려 요청 DTO입니다.
 */
@Getter
@Setter
public class ApprovalRequestProcessDto {
    private String rejectComment; // 반려 시 사유
}
