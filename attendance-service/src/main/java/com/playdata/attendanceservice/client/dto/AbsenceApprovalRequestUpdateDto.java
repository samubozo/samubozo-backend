package com.playdata.attendanceservice.client.dto;

import com.playdata.attendanceservice.absence.entity.AbsenceType;
import com.playdata.attendanceservice.absence.entity.UrgencyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbsenceApprovalRequestUpdateDto {
    private AbsenceType absenceType;
    private UrgencyType urgency;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String reason;
    private String title;
}