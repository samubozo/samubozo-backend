package com.playdata.attendanceservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyVacationStatsDto {

    private BigDecimal fullDayVacations;
    private BigDecimal halfDayVacations;
}
