package com.playdata.approvalservice.approval.dto;

import com.playdata.approvalservice.approval.entity.*;
import com.playdata.approvalservice.client.dto.DepartmentResDto;
import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
public class ApprovalRequestResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private RequestType requestType;
    private String applicantDepartment;
    private Long applicantId;
    private String applicantName; // 추가
    private Long approverId;
    private String approverName; // 추가
    private ApprovalStatus status;
    private LocalDate requestedAt;
    private LocalDate processedAt;
    private String reason;
    private String title; // 추가
    private Long vacationsId;
    private String vacationType; // 추가
    private Long certificatesId;
    private LocalDate startDate; // 추가
    private LocalDate endDate; // 추가
    private String rejectComment; // 추가

    // 부재 관련 필드들
    private Long absencesId;
    private AbsenceType absenceType;
    private UrgencyType urgency;
    private LocalTime startTime;
    private LocalTime endTime;

    public static ApprovalRequestResponseDto fromEntity(ApprovalRequest approvalRequest, String applicantName, String approverName,String applicantDepartment) {
        return ApprovalRequestResponseDto.builder()
                .id(approvalRequest.getId())
                .requestType(approvalRequest.getRequestType())
                .applicantDepartment(applicantDepartment)
                .applicantId(approvalRequest.getApplicantId())
                .applicantName(applicantName)
                .approverId(approvalRequest.getApproverId())
                .approverName(approverName)
                .status(approvalRequest.getStatus())
                .requestedAt(approvalRequest.getRequestedAt() != null ? approvalRequest.getRequestedAt().toLocalDate() : null)
                .processedAt(approvalRequest.getProcessedAt() != null ? approvalRequest.getProcessedAt().toLocalDate() : null)
                .reason(approvalRequest.getReason())
                .title(approvalRequest.getTitle())// 부서 정보 추가
                .vacationsId(approvalRequest.getVacationsId())
                .vacationType(approvalRequest.getVacationType()) // 추가
                .certificatesId(approvalRequest.getCertificateId())
                .startDate(approvalRequest.getStartDate()) // 추가
                .endDate(approvalRequest.getEndDate()) // 추가
                .rejectComment(approvalRequest.getRejectComment()) // 추가
                // 부재 관련 필드들 추가
                .absencesId(approvalRequest.getAbsencesId())
                .absenceType(approvalRequest.getAbsenceType())
                .urgency(approvalRequest.getUrgency())
                .startTime(approvalRequest.getStartTime())
                .endTime(approvalRequest.getEndTime())
                .build();
    }
}