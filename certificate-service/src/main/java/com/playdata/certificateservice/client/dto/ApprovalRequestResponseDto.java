package com.playdata.certificateservice.client.dto;

import lombok.Builder;
import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Builder
public class ApprovalRequestResponseDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String requestType;
    private Long applicantId;
    private String applicantName;
    private Long approverId;
    private String approverName;
    private String status;
    private LocalDate requestedAt;
    private LocalDate processedAt;
    private String reason;
    private String title;
    private Long vacationsId;
    private String vacationType;
    private Long certificatesId;
    private LocalDate startDate;
    private LocalDate endDate;
}
