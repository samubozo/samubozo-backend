package com.playdata.attendanceservice.attendance.absence.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 부재 종류를 나타내는 Enum
 * ANNUAL_LEAVE: 연차
 * HALF_DAY_LEAVE: 반차
 * SHORT_LEAVE: 외출
 * BUSINESS_TRIP: 출장
 * TRAINING: 연수
 * ETC: 기타
 */
@Getter
@RequiredArgsConstructor
public enum AbsenceType {
    ANNUAL_LEAVE("연차"),
    HALF_DAY_LEAVE("반차"),
    SHORT_LEAVE("외출"),
    BUSINESS_TRIP("출장"),
    TRAINING("연수"),
    ETC("기타");

    private final String description;
}
