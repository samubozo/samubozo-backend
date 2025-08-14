package com.playdata.attendanceservice.attendance.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PersonalAttendanceStatsDto {

    private final long attendanceCount;      // 총 출근 일수 (지각 포함)
    private final long lateCount;        // 지각
    private final long goOutCount;       // 외출
    private final BigDecimal halfDayVacationCount; // 반차
    private final BigDecimal fullDayVacationCount; // 연차
}
