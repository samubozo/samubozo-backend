package com.playdata.vacationservice.vacation.repository;

import com.playdata.vacationservice.vacation.entity.Vacation;
import com.playdata.vacationservice.vacation.entity.VacationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface VacationRepository extends JpaRepository<Vacation, Long> {

    /**
     * 특정 사용자의 특정 기간 동안의 승인된 휴가 기록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param startDate 시작일
     * @param endDate 종료일
     * @param status 휴가 상태
     * @return 휴가 목록
     */
    List<Vacation> findByUserIdAndStartDateBetweenAndVacationStatus(Long userId, LocalDate startDate, LocalDate endDate, VacationStatus status);

    /**
     * 특정 사용자의 모든 휴가 신청 내역을 최신순으로 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 휴가 목록
     */
    Page<Vacation> findByUserId(Long userId, Pageable pageable);

    /**
     * 특정 상태의 모든 휴가 신청 내역을 조회합니다.
     *
     * @param status 조회할 휴가 상태
     * @return 휴가 목록
     */
    List<Vacation> findByVacationStatus(VacationStatus status);

    /**
     * 여러 사용자의 특정 기간 동안의 승인된 유급 휴가 기록을 조회합니다.
     *
     * @param userIds 사용자 ID 목록
     * @param startDate 시작일
     * @param endDate 종료일
     * @param status 휴가 상태
     * @return 휴가 목록
     */
    List<Vacation> findByUserIdInAndStartDateBetweenAndVacationStatus(List<Long> userIds, LocalDate startDate, LocalDate endDate, VacationStatus status);
}