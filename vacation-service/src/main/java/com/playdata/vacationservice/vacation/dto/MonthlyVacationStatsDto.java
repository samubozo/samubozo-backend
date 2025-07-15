package com.playdata.vacationservice.vacation.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyVacationStatsDto {

    private BigDecimal fullDayVacations; // 연차 사용 횟수
    private BigDecimal halfDayVacations; // 반차 사용 횟수
}
