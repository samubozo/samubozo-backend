package com.playdata.attendanceservice.attendance.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 월별 근무일수 조회 결과를 담는 데이터 전송 객체(DTO)입니다.
 * JPQL 쿼리에서 new 키워드를 통해 직접 생성됩니다.
 */
@Getter
@AllArgsConstructor
public class MonthlyWorkDaysResponse {

    /**
     * 사용자 ID
     */
    private Long userId;

    /**
     * 계산된 근무일수
     */
    private Long workDayCount;
}
