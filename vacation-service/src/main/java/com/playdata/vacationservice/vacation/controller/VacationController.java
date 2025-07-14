package com.playdata.vacationservice.vacation.controller;

import com.playdata.vacationservice.common.auth.TokenUserInfo;
import com.playdata.vacationservice.vacation.dto.VacationRequestDto;
import com.playdata.vacationservice.vacation.dto.VacationBalanceResDto; // 추가
import com.playdata.vacationservice.vacation.service.VacationService;
import com.playdata.vacationservice.common.dto.CommonResDto; // 추가
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 휴가 관련 API 요청을 처리하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/vacations")
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

    /**
     * 특정 사용자의 연차 현황을 조회하는 API 엔드포인트입니다.
     *
     * @param userInfo 인증된 사용자 정보 (userId 획득용)
     * @return 연차 현황 정보를 담은 응답 (CommonResDto)
     */
    @GetMapping("/balance")
    public ResponseEntity<CommonResDto<VacationBalanceResDto>> getVacationBalance(@AuthenticationPrincipal TokenUserInfo userInfo) {
        try {
            VacationBalanceResDto vacationBalance = vacationService.getVacationBalance(userInfo.getEmployeeNo());
            return buildSuccessResponse(vacationBalance, "연차 현황 조회 성공");
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "연차 현황 조회 중 오류 발생");
        }
    }

    // CommonResDto를 위한 헬퍼 메소드
    private <T> ResponseEntity<CommonResDto<T>> buildSuccessResponse(T data, String message) {
        CommonResDto<T> resDto = new CommonResDto<>(HttpStatus.OK, message, data);
        return ResponseEntity.ok(resDto);
    }

    private <T> ResponseEntity<CommonResDto<T>> buildErrorResponse(HttpStatus status, String message) {
        CommonResDto<T> resDto = new CommonResDto<>(status, message, null);
        return ResponseEntity.status(status).body(resDto);
    }
}