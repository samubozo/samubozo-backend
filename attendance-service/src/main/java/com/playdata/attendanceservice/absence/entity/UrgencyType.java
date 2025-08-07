package com.playdata.attendanceservice.absence.entity;

import lombok.Getter;

@Getter
public enum UrgencyType {
    URGENT("긴급"),
    NORMAL("일반");

    private final String description;

    UrgencyType(String description) {
        this.description = description;
    }

}
