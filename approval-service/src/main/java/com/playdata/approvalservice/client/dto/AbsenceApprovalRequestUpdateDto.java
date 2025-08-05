package com.playdata.approvalservice.client.dto;

import com.playdata.approvalservice.approval.entity.AbsenceType;
import com.playdata.approvalservice.approval.entity.UrgencyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbsenceApprovalRequestUpdateDto {
    private Long approvalRequestId;
    private AbsenceType absenceType;
    private UrgencyType urgency;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;
    private String title; // 결재 요청의 제목도 업데이트 가능하도록 추가
}
