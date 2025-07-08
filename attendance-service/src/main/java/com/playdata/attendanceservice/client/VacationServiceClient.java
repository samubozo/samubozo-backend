package com.playdata.attendanceservice.client;

import com.playdata.attendanceservice.client.dto.VacationRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Vacation 서비스와 통신하기 위한 Feign 클라이언트 인터페이스입니다.
 */
@FeignClient(name = "vacation-service") // configuration 속성 추가
public interface VacationServiceClient {

    /**
     * Vacation 서비스에 휴가 신청을 요청합니다.
     *
     * @param requestDto 휴가 신청 정보
     * @return 휴가 신청 결과
     */
    @PostMapping("/api/v1/vacations/requestVacation")
    ResponseEntity<Void> requestVacation(
            @RequestBody VacationRequestDto requestDto);

    /**
     * 특정 사용자에게 지정된 일수만큼의 연차를 부여합니다。
     *
     * @param userId 연차를 부여할 사용자의 ID
     * @param days 부여할 연차 일수
     * @return 연차 부여 결과
     */
    @PutMapping("/api/v1/vacations/annual-leave/{userId}")
    ResponseEntity<Void> grantAnnualLeave(
            @PathVariable("userId") Long userId,
            @RequestParam("days") int days);

    /**
     * 특정 사용자에게 월별 정기 연차를 1일 부여합니다。
     *
     * @param userId 연차를 부여할 사용자의 ID
     * @return 연차 부여 결과
     */
    @PutMapping("/api/v1/vacations/monthly-leave/{userId}")
    ResponseEntity<Void> grantMonthlyLeave(
            @PathVariable("userId") Long userId);
}