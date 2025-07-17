package com.playdata.attendanceservice.client.dto;

public enum VacationStatus {
    PENDING_APPROVAL, // 승인 대기
    APPROVED,         // 승인됨
    REJECTED,         // 거부됨
    CANCELED          // 취소됨
}
