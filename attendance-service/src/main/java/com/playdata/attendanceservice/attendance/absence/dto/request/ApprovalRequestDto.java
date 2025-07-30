package com.playdata.attendanceservice.attendance.absence.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalRequestDto {
    private Long absenceId;
    private String requestType;
    private String applicantId;
    private String applicantDepartment;
    private String type;
    private String urgency;
    private String startDate;
    private String endDate;
    private String startTime;
    private String endTime;
    private String reason;
}