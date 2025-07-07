package com.playdata.attendanceservice.attendance.entity;

import lombok.Getter;

/**
 * 휴가 신청의 상태를 정의하는 Enum 입니다.
 */
@Getter
public enum VacationStatus {
    PENDING_APPROVAL("결재 대기"),
    APPROVED("승인"),
    REJECTED("반려");

    private final String description;

    VacationStatus(String description) {
        this.description = description;
    }
}