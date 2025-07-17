package com.playdata.vacationservice.vacation.dto;

import com.playdata.vacationservice.client.dto.UserResDto;
import com.playdata.vacationservice.vacation.entity.Vacation;
import com.playdata.vacationservice.vacation.entity.VacationType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

/**
 * 결재 대기 목록 조회의 응답으로 사용될 DTO입니다.
 */
@Getter
@Builder
public class PendingApprovalDto {

    private final Long vacationId;
    private final String applicantName;
    private final String department;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final VacationType vacationType;
    private final String reason;

    /**
     * Vacation 엔티티와 UserResDto를 조합하여 PendingApprovalDto를 생성합니다.
     *
     * @param vacation Vacation 엔티티
     * @param user     UserResDto 객체
     * @return PendingApprovalDto 객체
     */
    public static PendingApprovalDto from(Vacation vacation, UserResDto user) {
        return PendingApprovalDto.builder()
                .vacationId(vacation.getId())
                .applicantName(user.getUserName())
                .department(user.getDepartment() != null ? user.getDepartment().getName() : null)
                .startDate(vacation.getStartDate())
                .endDate(vacation.getEndDate())
                .vacationType(vacation.getVacationType())
                .reason(vacation.getReason())
                .build();
    }
}
