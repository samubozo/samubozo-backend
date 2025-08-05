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
    REGULAR("정상"),           // 정상 출근
    LATE("지각"),             // 지각
    EARLY_LEAVE("조퇴"),      // 조퇴
    ABSENCE("부재"),          // 일반 부재 (구체적이지 않은 경우)

    // 휴가 관련 (연차 잔여일수 차감)
    ANNUAL_LEAVE("연차"),     // 연차
    AM_HALF_DAY("오전 반차"), // 오전 반차
    PM_HALF_DAY("오후 반차"), // 오후 반차

    // 부재 관련 (급여 차감 없음)
    SICK_LEAVE("병가"),       // 병가
    OFFICIAL_LEAVE("공가"),   // 공가
    BUSINESS_TRIP("출장"),    // 출장
    TRAINING("연수"),         // 연수
    SHORT_LEAVE("외출"),      // 외출
    ETC("기타");              // 기타

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

    // 휴가인지 확인하는 메서드
    public boolean isVacation() {
        return this == ANNUAL_LEAVE || this == AM_HALF_DAY || this == PM_HALF_DAY;
    }

    // 부재인지 확인하는 메서드
    public boolean isAbsence() {
        return this == SICK_LEAVE || this == OFFICIAL_LEAVE ||
                this == BUSINESS_TRIP || this == TRAINING ||
                this == SHORT_LEAVE || this == ETC || this == ABSENCE;
    }

    // 연차 잔여일수 차감이 필요한지 확인하는 메서드
    public boolean requiresVacationBalanceDeduction() {
        return this == ANNUAL_LEAVE || this == AM_HALF_DAY || this == PM_HALF_DAY;
    }
}