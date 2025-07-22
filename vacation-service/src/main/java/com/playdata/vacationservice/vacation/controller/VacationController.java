package com.playdata.vacationservice.vacation.controller;

import com.playdata.vacationservice.common.auth.TokenUserInfo;
import com.playdata.vacationservice.common.dto.CommonResDto;
import com.playdata.vacationservice.vacation.dto.ApprovalRequestProcessDto;
import com.playdata.vacationservice.vacation.dto.PendingApprovalDto;
import com.playdata.vacationservice.vacation.dto.VacationHistoryResDto;
import com.playdata.vacationservice.vacation.dto.MonthlyVacationStatsDto;
import com.playdata.vacationservice.vacation.dto.VacationBalanceResDto;
import com.playdata.vacationservice.vacation.dto.VacationRequestDto;
import com.playdata.vacationservice.vacation.entity.Vacation;
import com.playdata.vacationservice.vacation.entity.VacationStatus;
import com.playdata.vacationservice.vacation.entity.VacationType;
import com.playdata.vacationservice.vacation.service.VacationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> requestVacation(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody VacationRequestDto requestDto) {
        vacationService.requestVacation(userInfo , requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 현재 로그인된 사용자의 모든 휴가 신청 내역을 조회합니다.
     *
     * @param userInfo 인증된 사용자 정보
     * @return 휴가 신청 내역 목록
     */
    @GetMapping("/my-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CommonResDto<List<VacationHistoryResDto>>> getMyVacationRequests(
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        List<VacationHistoryResDto> myRequests = vacationService.getMyVacationRequests(userInfo.getEmployeeNo());
        return buildSuccessResponse(myRequests, "내 휴가 신청 내역 조회 성공");
    }

    /**
     * 결재 대기 중인 모든 휴가 신청 목록을 조회합니다. (결재자용)
     *
     * @return 결재 대기 중인 휴가 신청 목록
     */
    @GetMapping("/pending-approvals")
    public ResponseEntity<CommonResDto<List<PendingApprovalDto>>> getPendingApprovals() {
        List<PendingApprovalDto> pendingApprovals = vacationService.getPendingApprovals();
        return buildSuccessResponse(pendingApprovals, "결재 대기 목록 조회 성공");
    }

    /**
     * 휴가 신청을 승인 처리합니다.
     *
     * @param vacationId 승인할 휴가 신청 ID
     * @return 성공 응답
     */
    @PostMapping("/{vacationId}/approve")
    public ResponseEntity<Void> approveVacation(@PathVariable Long vacationId) {
        vacationService.approveVacation(vacationId);
        return ResponseEntity.ok().build();
    }

    /**
     * 휴가 신청을 반려 처리합니다.
     *
     * @param vacationId 반려할 휴가 신청 ID
     * @param requestDto 반려 사유를 포함하는 DTO
     * @return 성공 응답
     */
    @PostMapping("/{vacationId}/reject")
    public ResponseEntity<Void> rejectVacation(@PathVariable Long vacationId, @RequestBody ApprovalRequestProcessDto requestDto) {
        vacationService.rejectVacation(vacationId, requestDto);
        return ResponseEntity.ok().build();
    }

    /**
     * 휴가 신청을 수정합니다. (PENDING_APPROVAL 상태만 가능)
     *
     * @param vacationId 수정할 휴가 신청 ID
     * @param userInfo 인증된 사용자 정보
     * @param requestDto 수정할 휴가 정보를 담은 DTO
     * @return 성공 응답
     */
    @PutMapping("/{vacationId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateVacationRequest(
            @PathVariable Long vacationId,
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody VacationRequestDto requestDto) {
        vacationService.updateVacationRequest(vacationId, userInfo.getEmployeeNo(), requestDto);
        return ResponseEntity.ok().build();
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

    /**
     * 특정 사용자의 월별 휴가 사용 통계를 조회합니다.
     *
     * @param userId 사용자 ID
     * @param year   조회할 연도
     * @param month  조회할 월
     * @return 월별 휴가 통계 응답
     */
    @GetMapping("/stats/{userId}/{year}/{month}")
    public ResponseEntity<CommonResDto<MonthlyVacationStatsDto>> getMonthlyVacationStats(
            @PathVariable Long userId,
            @PathVariable int year,
            @PathVariable int month) {
        try {
            MonthlyVacationStatsDto stats = vacationService.getMonthlyVacationStats(userId, year, month);
            return buildSuccessResponse(stats, "월별 휴가 통계 조회 성공");
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "월별 휴가 통계 조회 중 오류 발생");
        }
    }

    /**
     * 특정 사용자의 월별 반차 기록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param year   조회할 연도
     * @param month  조회할 월
     * @return 월별 반차 기록 목록
     */
    @GetMapping("/half-day/{userId}/{year}/{month}")
    public ResponseEntity<CommonResDto<List<Vacation>>> getMonthlyHalfDayVacations(
            @PathVariable Long userId,
            @PathVariable int year,
            @PathVariable int month) {
        try {
            List<Vacation> halfDayVacations = vacationService.getMonthlyHalfDayVacations(userId, year, month);
            return buildSuccessResponse(halfDayVacations, "월별 반차 기록 조회 성공");
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "월별 반차 기록 조회 중 오류 발생");
        }
    }

    /**
     * 특정 결재자가 처리한 모든 휴가 신청 내역을 조회합니다. (hrRole='Y' 사용자용)
     *
     * @param userInfo 인증된 사용자 정보
     * @return 처리된 휴가 신청 내역 목록
     */
    @GetMapping("/processed-approvals")
    @PreAuthorize("hasRole('HR')") // HR 역할만 접근 가능하도록 설정
    public ResponseEntity<CommonResDto<List<VacationHistoryResDto>>> getProcessedVacationApprovals(
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        List<VacationHistoryResDto> processedApprovals = vacationService.getProcessedVacationApprovals(userInfo);
        return buildSuccessResponse(processedApprovals, "처리된 휴가 신청 내역 조회 성공");
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

    /**
     * 결재 서비스로부터 휴가 상태 변경 통보를 받아 연차 잔액을 업데이트합니다.
     * 이 엔드포인트는 내부 서비스 간 통신을 위한 것이므로, 별도의 인증이 필요하지 않습니다.
     *
     * @param vacationId 휴가 ID
     * @param status 변경된 휴가 상태 (APPROVED 또는 REJECTED)
     * @param userId 해당 휴가 신청자의 ID
     * @param vacationType 휴가 종류
     * @param startDate 휴가 시작일
     * @param endDate 휴가 종료일
     * @return 성공 응답
     */
    @PostMapping("/internal/update-balance-on-approval")
    public ResponseEntity<Void> updateVacationBalanceOnApproval(
            @RequestParam("vacationId") Long vacationId,
            @RequestParam("status") String status,
            @RequestParam("userId") Long userId,
            @RequestParam("vacationType") String vacationTypeStr,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        VacationType vacationType = VacationType.valueOf(vacationTypeStr);
        VacationStatus newStatus = VacationStatus.valueOf(status);

        if (newStatus == VacationStatus.APPROVED) {
            BigDecimal deductionDays;
            if (vacationType == VacationType.ANNUAL_LEAVE) {
                long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
                deductionDays = new BigDecimal(daysBetween);
            } else {
                deductionDays = vacationType.getDeductionDays();
            }
            vacationService.deductVacationBalance(userId, deductionDays);
        } else if (newStatus == VacationStatus.REJECTED) {
            // 반려 시 연차 복구 로직은 필요 없음. 연차는 승인 시에만 차감되므로.
        }

        // 휴가 상태 업데이트
        vacationService.updateVacationStatus(vacationId, newStatus);

        return ResponseEntity.ok().build();
    }

    /**
     * 여러 사용자의 특정 기간 동안의 승인된 유급 휴가 일수를 조회합니다.
     * 내부 서비스 간 통신용 API입니다.
     *
     * @param userIds   조회할 사용자 ID 목록
     * @param startDate 조회 시작일
     * @param endDate   조회 종료일
     * @return 사용자 ID별 유급 휴가 일수 Map
     */
    @PostMapping("/internal/approved-paid-days")
    public ResponseEntity<CommonResDto<Map<Long, Double>>> getApprovedPaidVacationDays(
            @RequestBody List<Long> userIds,
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        try {
            Map<Long, Double> result = vacationService.getApprovedPaidVacationDaysForUsers(userIds, startDate, endDate);
            return buildSuccessResponse(result, "승인된 유급 휴가 일수 조회 성공");
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "승인된 유급 휴가 일수 조회 중 오류 발생");
        }
    }
}