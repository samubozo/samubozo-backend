package com.playdata.attendanceservice.attendance.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WorkDayType {
    FULL_DAY("전일"),
    HALF_DAY("반일");

    private final String description;
}
