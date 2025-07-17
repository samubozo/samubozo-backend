package com.playdata.approvalservice.approval.dto;

import com.playdata.approvalservice.approval.entity.ApprovalRequest;
import com.playdata.approvalservice.approval.entity.ApprovalStatus;
import com.playdata.approvalservice.approval.entity.RequestType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ApprovalRequestResponseDto {
    private Long id;
    private RequestType requestType;
    private Long applicantId;
    private String applicantName; // 추가
    private Long approverId;
    private String approverName; // 추가
    private ApprovalStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private String reason;
    private String title; // 추가
    private Long vacationsId;
    private Long certificatesId;

    public static ApprovalRequestResponseDto fromEntity(ApprovalRequest approvalRequest, String applicantName, String approverName) {
        return ApprovalRequestResponseDto.builder()
                .id(approvalRequest.getId())
                .requestType(approvalRequest.getRequestType())
                .applicantId(approvalRequest.getApplicantId())
                .applicantName(applicantName)
                .approverId(approvalRequest.getApproverId())
                .approverName(approverName)
                .status(approvalRequest.getStatus())
                .requestedAt(approvalRequest.getRequestedAt())
                .approvedAt(approvalRequest.getApprovedAt())
                .reason(approvalRequest.getReason())
                .title(approvalRequest.getTitle())
                .vacationsId(approvalRequest.getVacationsId())
                .certificatesId(approvalRequest.getCertificatesId())
                .build();
    }
}
