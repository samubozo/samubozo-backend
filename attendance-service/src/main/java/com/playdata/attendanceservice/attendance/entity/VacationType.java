package com.playdata.attendanceservice.attendance.entity;

import lombok.Getter;

import java.math.BigDecimal;

/**
 * 휴가 종류를 정의하는 Enum 입니다.
 * 각 휴가 종류는 차감할 연차 일수를 가지고 있습니다.
 */
@Getter
public enum VacationType {
    ANNUAL_LEAVE("연차", BigDecimal.ONE),
    AM_HALF_DAY("오전 반차", new BigDecimal("0.5")),
    PM_HALF_DAY("오후 반차", new BigDecimal("0.5"));

    private final String description;
    private final BigDecimal deductionDays;

    VacationType(String description, BigDecimal deductionDays) {
        this.description = description;
        this.deductionDays = deductionDays;
    }
}