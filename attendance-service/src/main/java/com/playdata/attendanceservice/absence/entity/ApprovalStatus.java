package com.playdata.attendanceservice.absence.entity;

import lombok.Getter;

@Getter
public enum ApprovalStatus {
    PENDING("대기"),
    APPROVED("승인"),
    REJECTED("반려");

    private final String description;

    ApprovalStatus(String description) {
        this.description = description;
    }

}
