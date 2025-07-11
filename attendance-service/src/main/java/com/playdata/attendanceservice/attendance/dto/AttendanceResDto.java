package com.playdata.attendanceservice.attendance.dto;

import com.playdata.attendanceservice.attendance.entity.Attendance;
import com.playdata.attendanceservice.attendance.entity.WorkDayType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AttendanceResDto {
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private LocalDateTime goOutTime;
    private LocalDateTime returnTime;
    private String workDayType; // WorkDayType을 String으로 변경

    public static AttendanceResDto from(Attendance attendance) {
        return AttendanceResDto.builder()
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .goOutTime(attendance.getGoOutTime())
                .returnTime(attendance.getReturnTime())
                .workDayType(attendance.getWorkStatus() != null && attendance.getWorkStatus().getWorkDayType() != null ? attendance.getWorkStatus().getWorkDayType().getDescription() : null)
                .build();
    }
}
