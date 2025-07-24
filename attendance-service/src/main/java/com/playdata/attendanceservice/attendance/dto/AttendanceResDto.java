package com.playdata.attendanceservice.attendance.dto;

import com.playdata.attendanceservice.attendance.entity.Attendance;
import com.playdata.attendanceservice.attendance.entity.WorkDayType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate; // LocalDate 임포트 추가
import java.time.LocalDateTime;

@Getter
@Builder
public class AttendanceResDto {
    private LocalDate attendanceDate; // attendanceDate 필드 추가
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private LocalDateTime goOutTime;
    private LocalDateTime returnTime;
    private String workDayType; // WorkDayType을 String으로 변경
    private String totalWorkTime;
    private String normalWorkTime;
    private String overtimeWorkTime;
    private String nightWorkTime;

    public static AttendanceResDto from(Attendance attendance, String totalWorkTime, String normalWorkTime, String overtimeWorkTime, String nightWorkTime) {
        return AttendanceResDto.builder()
                .attendanceDate(attendance.getAttendanceDate()) // attendanceDate 설정 추가
                .checkInTime(attendance.getCheckInTime())
                .checkOutTime(attendance.getCheckOutTime())
                .goOutTime(attendance.getGoOutTime())
                .returnTime(attendance.getReturnTime())
                .workDayType(attendance.getWorkStatus() != null && attendance.getWorkStatus().getWorkDayType() != null ? attendance.getWorkStatus().getWorkDayType().getDescription() : null)
                .totalWorkTime(totalWorkTime)
                .normalWorkTime(normalWorkTime)
                .overtimeWorkTime(overtimeWorkTime)
                .nightWorkTime(nightWorkTime)
                .build();
    }
}
