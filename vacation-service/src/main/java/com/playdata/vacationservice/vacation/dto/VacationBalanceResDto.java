package com.playdata.vacationservice.vacation.dto;

import com.playdata.vacationservice.vacation.entity.VacationBalance;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class VacationBalanceResDto {
    private Long userId;
    private BigDecimal totalGranted;
    private BigDecimal usedDays;
    private BigDecimal remainingDays;

    public static VacationBalanceResDto from(VacationBalance vacationBalance) {
        return VacationBalanceResDto.builder()
                .userId(vacationBalance.getUserId())
                .totalGranted(vacationBalance.getTotalGranted())
                .usedDays(vacationBalance.getUsedDays())
                .remainingDays(vacationBalance.getRemainingDays())
                .build();
    }
}