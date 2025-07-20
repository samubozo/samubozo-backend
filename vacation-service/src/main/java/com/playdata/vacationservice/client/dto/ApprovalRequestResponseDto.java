package com.playdata.vacationservice.client.dto;

import com.playdata.vacationservice.vacation.entity.RequestType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ApprovalRequestResponseDto {
    private Long id;
    private RequestType requestType;
    private Long applicantId;
    private String applicantName;
    private Long approverId;
    private String approverName;
    private String status;
    private LocalDate requestedAt;
    private LocalDate processedAt; // approvedAt -> processedAt 변경
    private String reason;
    private String title;
    private Long vacationsId;
    private Long certificatesId;
}