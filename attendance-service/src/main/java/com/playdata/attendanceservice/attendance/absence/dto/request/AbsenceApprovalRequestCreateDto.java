package com.playdata.attendanceservice.attendance.absence.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.playdata.attendanceservice.attendance.absence.entity.AbsenceType;
import com.playdata.attendanceservice.attendance.absence.entity.UrgencyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AbsenceApprovalRequestCreateDto {
    private Long absencesId;
    private AbsenceType absenceType;
    private UrgencyType urgency;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private String reason;
    private String purpose;
}