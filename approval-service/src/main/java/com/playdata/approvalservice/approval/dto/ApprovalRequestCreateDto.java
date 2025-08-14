package com.playdata.approvalservice.approval.dto;

import com.playdata.approvalservice.approval.entity.AbsenceType;
import com.playdata.approvalservice.approval.entity.RequestType;
import com.playdata.approvalservice.approval.entity.Type;
import com.playdata.approvalservice.approval.entity.UrgencyType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 모든 결재 요청 생성을 위한 범용 DTO입니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ApprovalRequestCreateDto {

    private RequestType requestType;
    private Long applicantId;
    private String title;
    private String reason;
    private Long approverId;

    // Vacation 관련 필드
    private Long vacationsId;
    private String vacationType;

    // Certificate 관련 필드
    private Long certificateId;
    private Type certificateType;

    // Absence 관련 필드
    private Long absencesId;
    private AbsenceType absenceType;
    private UrgencyType urgency;

    // 공통 날짜/시간 필드
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
}