package com.playdata.attendanceservice.attendance.entity;

public enum WorkStatusType {
    REGULAR, // 정상 출근
    LATE,  // 지각
    EARLY_LEAVE,  //조퇴
    ABSENCE, // 부재
    OUT_OF_OFFICE // 외출
}
