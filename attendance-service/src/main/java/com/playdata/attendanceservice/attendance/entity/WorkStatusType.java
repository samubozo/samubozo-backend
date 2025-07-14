package com.playdata.attendanceservice.attendance.entity;

public enum WorkStatusType {
    REGULAR, // 정상 출근
    LATE,  // 지각
    EARLY_LEAVE,  // 조퇴
    ABSENCE, // 일반 부재 (병가, 경조사 등 구체적이지 않은 경우)
    OUT_OF_OFFICE, // 외출

    // 사용자 요청에 따른 추가 부재 유형
    BUSINESS_TRIP, // 출장
    TRAINING,      // 연수
    ANNUAL_LEAVE,  // 연차
    HALF_DAY_LEAVE // 반차
}
