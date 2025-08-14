package com.playdata.certificateservice.client.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class ApprovalRequestCreateDto {
    private RequestType requestType;
    private Long applicantId;
    private String title;
    private String reason;
    private Long vacationsId;
    private String vacationType;
    private Long certificateId;
    private LocalDate startDate;
    private LocalDate endDate;
}
