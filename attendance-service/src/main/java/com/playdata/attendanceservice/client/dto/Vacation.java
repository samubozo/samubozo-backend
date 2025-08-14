package com.playdata.attendanceservice.client.dto;

import com.playdata.attendanceservice.attendance.entity.VacationType;
import com.playdata.attendanceservice.client.dto.VacationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Vacation {
    private Long id;
    private Long userId;
    private VacationType vacationType;
    private VacationStatus vacationStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}
