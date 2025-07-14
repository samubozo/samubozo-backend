package com.playdata.vacationservice.vacation.controller;

import com.playdata.vacationservice.common.auth.TokenUserInfo;
import com.playdata.vacationservice.vacation.dto.VacationRequestDto;
import com.playdata.vacationservice.vacation.service.VacationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 휴가 관련 API 요청을 처리하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/api/v1/vacations")
@RequiredArgsConstructor
public class VacationController {

    private final VacationService vacationService;

    /**
     * 새로운 휴가 신청을 등록합니다.
     *
     * @param userInfo   인증된 사용자 정보
     * @param requestDto 휴가 신청에 필요한 정보를 담은 DTO
     * @return 성공 시 HTTP 201 Created 응답
     */
    @PostMapping("/requestVacation")
    public ResponseEntity<Void> requestVacation(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody VacationRequestDto requestDto) {
        vacationService.requestVacation(userInfo , requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 휴가 신청 관련 예외를 처리합니다.
     *
     * @param ex 발생한 예외
     * @return 에러 메시지와 함께 HTTP 400 Bad Request 응답
     */
    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<String> handleVacationException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}