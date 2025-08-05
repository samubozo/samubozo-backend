package com.playdata.attendanceservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VacationWorkStatusRequestDto {
    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String vacationType;
    private String reason;
}
