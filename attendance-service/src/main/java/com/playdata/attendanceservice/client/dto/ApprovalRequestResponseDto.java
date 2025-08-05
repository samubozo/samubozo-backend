package com.playdata.attendanceservice.client.dto;

import com.playdata.attendanceservice.attendance.absence.entity.AbsenceType;
import com.playdata.attendanceservice.attendance.absence.entity.UrgencyType;
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
    private String applicantName;
    private Long approverId;
    private String approverName;
    private ApprovalStatus status;
    private LocalDate requestedAt;
    private LocalDate processedAt;
    private String reason;
    private String title;
    private Long vacationsId;
    private String vacationType;
    private Long certificatesId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String rejectComment;

    // 부재 관련 필드들
    private Long absencesId;
    private AbsenceType absenceType;
    private UrgencyType urgency;
    private LocalTime startTime;
    private LocalTime endTime;
}
