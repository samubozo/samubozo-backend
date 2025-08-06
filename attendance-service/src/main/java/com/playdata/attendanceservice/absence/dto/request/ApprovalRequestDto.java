package com.playdata.attendanceservice.absence.dto.request;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalRequestDto {
    // setter 추가
    @Setter
    private Long absenceId;
    private String requestType;
    private Long applicantId;
    private String applicantDepartment;
    private String type;
    private String urgency;
    private String startDate;
    private String endDate;
    private String startTime;
    private String endTime;
    private String reason;

}
