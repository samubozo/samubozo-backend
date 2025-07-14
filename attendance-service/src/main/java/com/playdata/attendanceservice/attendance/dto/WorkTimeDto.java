package com.playdata.attendanceservice.attendance.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WorkTimeDto {
    private String remainingHours;
    private String workedHours;

    public WorkTimeDto(String remainingHours, String workedHours) {
        this.remainingHours = remainingHours;
        this.workedHours = workedHours;
    }

}
