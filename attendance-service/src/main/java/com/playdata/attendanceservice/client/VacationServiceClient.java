package com.playdata.attendanceservice.client;

import com.playdata.attendanceservice.client.dto.MonthlyVacationStatsDto;
import com.playdata.attendanceservice.client.dto.Vacation;
import com.playdata.attendanceservice.client.dto.VacationBalanceResDto;
import com.playdata.attendanceservice.client.dto.VacationRequestDto;
import com.playdata.attendanceservice.common.configs.FeignClientConfig;
import com.playdata.attendanceservice.common.dto.CommonResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Vacation 서비스와 통신하기 위한 Feign 클라이언트 인터페이스입니다.
 */
@FeignClient(name = "vacation-service", configuration = FeignClientConfig.class)
public interface VacationServiceClient {

    /**
     * Vacation 서비스에 휴가 신청을 요청합니다.
     *
     * @param requestDto 휴가 신청 정보
     * @return 휴가 신청 결과
     */
    @PostMapping("/vacations/requestVacation")
    ResponseEntity<Void> requestVacation(
            @RequestBody VacationRequestDto requestDto);

    /**
     * 특정 사용자에게 지정된 일수만큼의 연차를 부여합니다.
     *
     * @param userId 연차를 부여할 사용자의 ID
     * @param days 부여할 연차 일수
     * @return 연차 부여 결과
     */
    @PutMapping("/vacations/annual-leave/{userId}")
    ResponseEntity<Void> grantAnnualLeave(
            @PathVariable("userId") Long userId,
            @RequestParam("days") int days);

    /**
     * 특정 사용자에게 월별 정기 연차를 1일 부여합니다.
     *
     * @param userId 연차를 부여할 사용자의 ID
     * @return 연차 부여 결과
     */
    @PutMapping("/vacations/monthly-leave/{userId}")
    ResponseEntity<Void> grantMonthlyLeave(
            @PathVariable("userId") Long userId);

    /**
     * 특정 사용자의 월별 휴가 사용 통계를 조회합니다.
     *
     * @param userId 사용자 ID
     * @param year   조회할 연도
     * @param month  조회할 월
     * @return 월별 휴가 통계
     */
    @GetMapping("/vacations/stats/{userId}/{year}/{month}")
    CommonResDto<MonthlyVacationStatsDto> getMonthlyVacationStats(
            @PathVariable("userId") Long userId,
            @PathVariable("year") int year,
            @PathVariable("month") int month);

    /**
     * 특정 사용자의 연차 현황을 조회합니다.
     *
     * @param userId 사용자 ID
     * @return 연차 현황
     */
    @GetMapping("/vacations/balance")
    CommonResDto<VacationBalanceResDto> getVacationBalance(@RequestParam("userId") Long userId);

    /**
     * 특정 사용자의 월별 반차 기록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param year   조회할 연도
     * @param month  조회할 월
     * @return 월별 반차 기록 목록
     */
    @GetMapping("/vacations/half-day/{userId}/{year}/{month}")
    CommonResDto<List<Vacation>> getMonthlyHalfDayVacations(
            @PathVariable("userId") Long userId,
            @PathVariable("year") int year,
            @PathVariable("month") int month);
}