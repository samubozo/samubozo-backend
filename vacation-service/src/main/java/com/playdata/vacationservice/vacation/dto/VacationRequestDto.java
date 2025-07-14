package com.playdata.vacationservice.vacation.dto;

import com.playdata.vacationservice.vacation.entity.VacationType; // 경로 변경
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 휴가 신청 요청을 위한 DTO 입니다.
 */
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class VacationRequestDto {

    private VacationType vacationType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}