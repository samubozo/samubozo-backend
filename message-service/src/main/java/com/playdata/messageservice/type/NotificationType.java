package com.playdata.messageservice.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    MESSAGE("쪽지"),
    ATTENDANCE("근태"),
    E_APPROVAL("전자결재");

    private final String description; // 알림 유형에 대한 설명
}
