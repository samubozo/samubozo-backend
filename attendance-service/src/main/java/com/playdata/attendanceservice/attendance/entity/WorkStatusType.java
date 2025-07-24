package com.playdata.attendanceservice.attendance.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@RequiredArgsConstructor
public enum WorkStatusType {
    REGULAR("정상"), // 정상 출근
    LATE("지각"),  // 지각
    EARLY_LEAVE("조퇴"),  // 조퇴
    ABSENCE("부재"), // 일반 부재 (병가, 경조사 등 구체적이지 않은 경우)
    BUSINESS_TRIP("출장"), // 출장
    TRAINING("연수"), // 연수
    HALF_DAY_LEAVE("반차"), // 반차
    OUT_OF_OFFICE("외출"); // 외출

    private final String description;

    // description을 통해 Enum을 찾기 위한 맵
    private static final Map<String, WorkStatusType> BY_DESCRIPTION =
            Arrays.stream(values()).collect(Collectors.toMap(WorkStatusType::getDescription, Function.identity()));

    @JsonCreator
    public static WorkStatusType fromDescription(String description) {
        return Optional.ofNullable(BY_DESCRIPTION.get(description))
                .orElseThrow(() -> new IllegalArgumentException("Unknown WorkStatusType description: " + description));
    }

    @JsonValue
    public String getDescription() {
        return description;
    }
}
