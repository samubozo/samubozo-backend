package com.playdata.attendanceservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class VacationBalanceResDto {
    private BigDecimal totalGranted;
    private BigDecimal usedDays;
    private BigDecimal remainingDays;
}