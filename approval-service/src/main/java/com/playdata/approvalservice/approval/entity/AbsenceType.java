package com.playdata.approvalservice.approval.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
}