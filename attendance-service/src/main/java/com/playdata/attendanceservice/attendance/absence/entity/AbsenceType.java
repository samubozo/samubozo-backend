package com.playdata.attendanceservice.attendance.absence.entity;

import com.playdata.attendanceservice.attendance.entity.WorkStatusType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 부재 종류를 나타내는 Enum
 * 모든 부재는 전자결재 대상입니다.
 *
 * SICK_LEAVE: 병가 (전자결재 대상)
 * OFFICIAL_LEAVE: 공가 (전자결재 대상)
 * SHORT_LEAVE: 외출 (전자결재 대상)
 * BUSINESS_TRIP: 출장 (전자결재 대상)
 * TRAINING: 연수 (전자결재 대상)
 * ETC: 기타 (전자결재 대상)
 *
 * ANNUAL_LEAVE와 HALF_DAY_LEAVE는 휴가(VacationType)로 이동
 */
@Getter
@RequiredArgsConstructor
public enum AbsenceType {
    SICK_LEAVE("병가"),
    OFFICIAL_LEAVE("공가"),
    SHORT_LEAVE("외출"),
    BUSINESS_TRIP("출장"),
    TRAINING("연수"),
    ETC("기타");

    private final String description;

    // 모든 부재는 전자결재 대상
    public boolean requiresApproval() {
        return true; // 모든 부재는 승인 필요
    }

    // 전자결재 대상이 아닌지 확인하는 메서드
    public boolean isSelfApproved() {
        return false; // 모든 부재는 승인 필요
    }

    // WorkStatusType으로 변환하는 메서드
    public WorkStatusType toWorkStatusType() {
        switch (this) {
            case SICK_LEAVE:
                return WorkStatusType.SICK_LEAVE;
            case OFFICIAL_LEAVE:
                return WorkStatusType.OFFICIAL_LEAVE;
            case BUSINESS_TRIP:
                return WorkStatusType.BUSINESS_TRIP;
            case TRAINING:
                return WorkStatusType.TRAINING;
            case SHORT_LEAVE:
                return WorkStatusType.SHORT_LEAVE;
            case ETC:
                return WorkStatusType.ETC;
            default:
                throw new IllegalArgumentException("Invalid absence type: " + this);
        }
    }
}