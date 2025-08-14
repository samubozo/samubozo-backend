package com.playdata.approvalservice.approval.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RequestType {
    VACATION("연차/반차"),
    CERTIFICATE("증명서"),
    ABSENCE("부재");// 부재 추가

    private final String description;
}