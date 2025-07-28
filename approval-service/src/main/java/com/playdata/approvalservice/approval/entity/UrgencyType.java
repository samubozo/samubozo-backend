package com.playdata.approvalservice.approval.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UrgencyType {
    URGENT("긴급"),
    NORMAL("일반");

    private final String description;
}