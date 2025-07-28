package com.playdata.attendanceservice.attendance.absence.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 부재 종류를 나타내는 Enum
 * SICK_LEAVE: 병가 (전자결재 대상)
 * OFFICIAL_LEAVE: 공가 (전자결재 대상)
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
    SICK_LEAVE("병가"),
    OFFICIAL_LEAVE("공가"),
    ANNUAL_LEAVE("연차"),
    HALF_DAY_LEAVE("반차"),
    SHORT_LEAVE("외출"),
    BUSINESS_TRIP("출장"),
    TRAINING("연수"),
    ETC("기타");

    private final String description;

    // 전자결재 대상인지 확인하는 메서드
    public boolean requiresApproval() {
        return this == SICK_LEAVE || this == OFFICIAL_LEAVE;
    }

    // 전자결재 대상이 아닌지 확인하는 메서드
    public boolean isSelfApproved() {
        return !requiresApproval();
    }
}