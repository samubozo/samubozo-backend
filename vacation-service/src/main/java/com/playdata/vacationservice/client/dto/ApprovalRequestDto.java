package com.playdata.vacationservice.client.dto;

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

    private String approvalType; // 결재 종류 (예: "VACATION")
    private Long userId;
    private String title; // 결재 문서 제목 (예: "[휴가신청] 홍길동")
    private String content; // 결재 내용 (HTML 또는 텍스트)
    private Long referenceId; // 관련 문서 ID (여기서는 Vacation의 ID)
}