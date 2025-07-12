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
    private Long approverId;
    private ApprovalStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private String reason;
    private Long vacationsId;
    private Long certificatesId;

    public static ApprovalRequestResponseDto fromEntity(ApprovalRequest approvalRequest) {
        return ApprovalRequestResponseDto.builder()
                .id(approvalRequest.getId())
                .requestType(approvalRequest.getRequestType())
                .applicantId(approvalRequest.getApplicantId())
                .approverId(approvalRequest.getApproverId())
                .status(approvalRequest.getStatus())
                .requestedAt(approvalRequest.getRequestedAt())
                .approvedAt(approvalRequest.getApprovedAt())
                .reason(approvalRequest.getReason())
                .vacationsId(approvalRequest.getVacationsId())
                .certificatesId(approvalRequest.getCertificatesId())
                .build();
    }
}
