package com.playdata.vacationservice.vacation.dto;

import com.playdata.vacationservice.vacation.entity.Vacation;
import com.playdata.vacationservice.vacation.entity.VacationStatus;
import com.playdata.vacationservice.vacation.entity.VacationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 내 휴가 신청 내역 조회의 응답으로 사용될 DTO입니다.
 */
@Getter
@Builder
public class VacationHistoryResDto {

    private final Long id;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final VacationType vacationType;
    private final VacationStatus vacationStatus;
    private final String reason;

    /**
     * Vacation 엔티티를 VacationHistoryResDto로 변환합니다.
     *
     * @param vacation Vacation 엔티티
     * @return VacationHistoryResDto 객체
     */
    public static VacationHistoryResDto from(Vacation vacation) {
        return VacationHistoryResDto.builder()
                .id(vacation.getId())
                .startDate(vacation.getStartDate())
                .endDate(vacation.getEndDate())
                .vacationType(vacation.getVacationType())
                .vacationStatus(vacation.getVacationStatus())
                .reason(vacation.getReason())
                .build();
    }
}
