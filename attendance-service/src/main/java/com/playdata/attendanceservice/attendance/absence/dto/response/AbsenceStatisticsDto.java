package com.playdata.attendanceservice.attendance.absence.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AbsenceStatisticsDto {
    private long totalAbsences;
    private long pendingAbsences;
    private long approvedAbsences;
    private long rejectedAbsences;
}