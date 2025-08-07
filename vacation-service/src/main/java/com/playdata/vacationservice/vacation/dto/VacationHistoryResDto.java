package com.playdata.vacationservice.vacation.dto;

import lombok.Builder;
import lombok.Getter;
import com.playdata.vacationservice.vacation.entity.Vacation;
import com.playdata.vacationservice.vacation.entity.VacationStatus;
import com.playdata.vacationservice.vacation.entity.VacationType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 내 휴가 신청 내역 조회의 응답으로 사용될 DTO입니다.
 */
@Getter
@Builder
public class VacationHistoryResDto {

    private final Long id;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String period; // 추가된 기간 필드
    private final VacationType vacationType;
    private final VacationStatus vacationStatus;
    private final String reason;
    private final LocalDate requestedAt;
    private final String applicantDepartment; // 신청자 부서 추가
    private final String approverName;
    private final Long approverEmployeeNo;
    private final java.time.LocalDate processedAt;
    private final String rejectComment; // <-- 이 필드를 추가합니다.

    /**
     * Vacation 엔티티를 VacationHistoryResDto로 변환합니다.
     *
     * @param vacation Vacation 엔티티
     * @return VacationHistoryResDto 객체
     */
    public static VacationHistoryResDto from(Vacation vacation) {
        String periodString = String.format("%s ~ %s",
                vacation.getStartDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                vacation.getEndDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
        );
        return VacationHistoryResDto.builder()
                .id(vacation.getId())
                .startDate(vacation.getStartDate())
                .endDate(vacation.getEndDate())
                .period(periodString)
                .vacationType(vacation.getVacationType())
                .vacationStatus(vacation.getVacationStatus())
                .reason(vacation.getReason())
                .requestedAt(vacation.getCreatedAt() != null ? vacation.getCreatedAt().toLocalDate() : null)
                .rejectComment(vacation.getRejectComment()) // <-- 이 라인을 추가합니다.
                .build();
    }

    /**
     * Vacation 엔티티와 결재 정보를 VacationHistoryResDto로 변환합니다.
     *
     * @param vacation Vacation 엔티티
     * @param approverName 결재자 이름
     * @param approverEmployeeNo 결재자 사번
     * @param processedAt 결재 처리 일시
     * @param applicantDepartment 신청자 부서
     * @return VacationHistoryResDto 객체
     */
    public static VacationHistoryResDto from(Vacation vacation, String approverName, Long approverEmployeeNo, java.time.LocalDate processedAt, String applicantDepartment) {
        String periodString = String.format("%s ~ %s",
                vacation.getStartDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")),
                vacation.getEndDate().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"))
        );
        return VacationHistoryResDto.builder()
                .id(vacation.getId())
                .startDate(vacation.getStartDate())
                .endDate(vacation.getEndDate())
                .period(periodString)
                .vacationType(vacation.getVacationType())
                .vacationStatus(vacation.getVacationStatus())
                .reason(vacation.getReason())
                .requestedAt(vacation.getCreatedAt() != null ? vacation.getCreatedAt().toLocalDate() : null)
                .approverName(approverName)
                .approverEmployeeNo(approverEmployeeNo)
                .processedAt(processedAt)
                .applicantDepartment(applicantDepartment)
                .rejectComment(vacation.getRejectComment()) // <-- 이 라인을 추가합니다.
                .build();
    }
}