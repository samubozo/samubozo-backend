package com.playdata.approvalservice.approval.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AbsenceApprovalStatisticsDto {
    private long totalAbsenceRequests;
    private long pendingAbsenceRequests;
    private long approvedAbsenceRequests;
    private long rejectedAbsenceRequests;
}