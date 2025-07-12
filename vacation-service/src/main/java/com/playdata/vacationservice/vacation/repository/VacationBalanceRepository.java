package com.playdata.vacationservice.vacation.repository;

import com.playdata.vacationservice.vacation.entity.VacationBalance; // 경로 변경
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * VacationBalance 엔티티의 데이터베이스 접근을 위한 리포지토리 인터페이스입니다.
 */
@Repository
public interface VacationBalanceRepository extends JpaRepository<VacationBalance, Long> {

    /**
     * 특정 사용자의 연차 정보를 조회합니다.
     *
     * @param userId 조회할 사용자의 ID
     * @return 해당 사용자의 VacationBalance 엔티티 (존재하지 않을 경우 Optional.empty())
     */
    Optional<VacationBalance> findByUserId(Long userId);
}