package com.playdata.payrollservice.payroll.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class AttendanceResDto {
    private LocalDate attendanceDate;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private String totalWorkTime; // 예: "08:30"
    private String normalWorkTime;
    private String overtimeWorkTime;
    private String nightWorkTime;
}
