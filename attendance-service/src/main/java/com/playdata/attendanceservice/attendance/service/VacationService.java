package com.playdata.attendanceservice.attendance.service;

import com.playdata.attendanceservice.attendance.entity.VacationBalance;
import com.playdata.attendanceservice.attendance.repository.VacationBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 연차/휴가 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VacationService {

    private final VacationBalanceRepository vacationBalanceRepository;

    /**
     * 특정 사용자에게 월별 정기 연차를 1일 부여합니다.
     * 만약 해당 사용자의 연차 정보가 존재하지 않으면 새로 생성하여 1일을 부여합니다.
     *
     * @param userId 연차를 부여할 사용자의 ID
     */
    @Transactional
    public void grantMonthlyLeave(Long userId) {
        // 사용자 ID로 연차 정보를 조회하거나, 없으면 새로 생성합니다.
        VacationBalance vacationBalance = vacationBalanceRepository.findByUserId(userId)
                .orElse(VacationBalance.builder()
                        .userId(userId)
                        .totalGranted(java.math.BigDecimal.ZERO)
                        .usedDays(java.math.BigDecimal.ZERO)
                        .build());

        // 연차를 1일 부여합니다.
        vacationBalance.grantDays(java.math.BigDecimal.ONE);

        // 변경된 연차 정보를 저장합니다.
        vacationBalanceRepository.save(vacationBalance);

        log.info("사용자 ID: {} 에게 연차 1일이 부여되었습니다. 총 부여된 연차: {}", userId, vacationBalance.getTotalGranted());
    }
}