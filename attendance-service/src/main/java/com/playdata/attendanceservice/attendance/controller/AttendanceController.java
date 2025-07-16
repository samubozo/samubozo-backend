package com.playdata.attendanceservice.attendance.controller;

import com.playdata.attendanceservice.attendance.dto.AttendanceResDto;
import com.playdata.attendanceservice.attendance.dto.PersonalAttendanceStatsDto;
import com.playdata.attendanceservice.attendance.dto.WorkTimeDto;
import com.playdata.attendanceservice.attendance.entity.Attendance;
import com.playdata.attendanceservice.attendance.service.AttendanceService;
import com.playdata.attendanceservice.client.VacationServiceClient;
import com.playdata.attendanceservice.common.auth.TokenUserInfo;
import com.playdata.attendanceservice.common.dto.CommonResDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
@Slf4j
@RefreshScope
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final VacationServiceClient vacationServiceClient;

    /**
     * 사용자의 출근을 기록하는 API 엔드포인트입니다.
     *
     * @param request HttpServletRequest 객체 (클라이언트 IP 주소 획득용)
     * @return 출근 기록 성공 또는 실패에 대한 응답 (CommonResDto)
     */
    @PostMapping("/check-in")
    public ResponseEntity<CommonResDto<Attendance>> checkIn(@AuthenticationPrincipal TokenUserInfo userInfo, HttpServletRequest request) {
        try {
            String ipAddress = request.getRemoteAddr();
            Attendance attendance = attendanceService.recordCheckIn(userInfo.getEmployeeNo(), ipAddress);
            return buildSuccessResponse(attendance, "출근 기록 성공");
        } catch (IllegalStateException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "출근 기록 중 오류 발생");
        }
    }

    /**
     * 사용자의 퇴근을 기록하는 API 엔드포인트입니다.
     *
     * @return 퇴근 기록 성공 또는 실패에 대한 응답 (CommonResDto)
     */
    @PostMapping("/check-out")
    public ResponseEntity<CommonResDto<Attendance>> checkOut(@AuthenticationPrincipal TokenUserInfo userInfo) {
        try {
            Attendance attendance = attendanceService.recordCheckOut(userInfo.getEmployeeNo());
            return buildSuccessResponse(attendance, "퇴근 기록 성공");
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "퇴근 기록 중 오류 발생");
        }
    }

    /**
     * 특정 사용자의 월별 근태 기록을 조회하는 API 엔드포인트입니다.
     *
     * @param year 조회할 연도 (URL 경로에서 추출)
     * @param month 조회할 월 (URL 경로에서 추출)
     * @return 월별 근태 기록 목록 또는 오류에 대한 응답 (CommonResDto)
     */
    @GetMapping("/monthly/{year}/{month}")
    public ResponseEntity<CommonResDto<List<Attendance>>> getMonthlyAttendance(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable int year,
            @PathVariable int month) {
        try {
            List<Attendance> monthlyAttendances = attendanceService.getMonthlyAttendances(userInfo.getEmployeeNo(), year, month);
            return buildSuccessResponse(monthlyAttendances, "월별 근태 조회 성공");
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "월별 근태 조회 중 오류 발생");
        }
    }

    @PutMapping("/go-out")
    public ResponseEntity<CommonResDto<AttendanceResDto>> goOut(@AuthenticationPrincipal TokenUserInfo userInfo) {
        try {
            Attendance attendance = attendanceService.recordGoOut(userInfo.getEmployeeNo());
            return buildSuccessResponse(AttendanceResDto.from(attendance), "외출 기록 성공");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "외출 기록 중 오류 발생");
        }
    }

    @PutMapping("/return")
    public ResponseEntity<CommonResDto<AttendanceResDto>> returnFromGoOut(@AuthenticationPrincipal TokenUserInfo userInfo) {
        try {
            Attendance attendance = attendanceService.recordReturn(userInfo.getEmployeeNo());
            return buildSuccessResponse(AttendanceResDto.from(attendance), "복귀 기록 성공");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "복귀 기록 중 오류 발생");
        }
    }

    /**
     * 사용자의 남은 근무 시간을 조회하는 API 엔드포인트입니다.
     *
     * @param userInfo 인증된 사용자의 정보 (userId 획득용)
     * @return 남은 근무 시간 문자열 또는 오류에 대한 응답 (CommonResDto)
     */
    @GetMapping("/remaining-work-time")
    public ResponseEntity<CommonResDto<WorkTimeDto>> getRemainingWorkTime(@AuthenticationPrincipal TokenUserInfo userInfo) {
        try {
            WorkTimeDto remainingTime = attendanceService.getRemainingWorkTime(userInfo.getEmployeeNo());
            return buildSuccessResponse(remainingTime, "남은 근무 시간 조회 성공");
        } catch (Exception e) {
            log.error("남은 근무 시간 조회 중 오류 발생: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "남은 근무 시간 조회 중 오류 발생");
        }
    }

    /**
     * 특정 사용자의 오늘 출근 기록을 조회하는 API 엔드포인트입니다.
     * 로그인 후 또는 페이지 로드 시 현재 사용자의 출퇴근, 외출/복귀 시간을 프론트엔드에 제공합니다.
     *
     * @param userInfo 인증된 사용자의 정보 (userId 획득용)
     * @return 오늘 출근 기록이 있다면 AttendanceResDto, 없다면 null을 포함한 CommonResDto
     */
    @GetMapping("/today")
    public ResponseEntity<CommonResDto<AttendanceResDto>> getTodayAttendance(@AuthenticationPrincipal TokenUserInfo userInfo) {
        try {
            Optional<AttendanceResDto> todayAttendance = attendanceService.getTodayAttendance(userInfo.getEmployeeNo());
            if (todayAttendance.isPresent()) {
                return buildSuccessResponse(todayAttendance.get(), "오늘 근태 기록 조회 성공");
            } else {
                return buildSuccessResponse(null, "오늘 근태 기록 없음");
            }
        } catch (Exception e) {
            log.error("오늘 근태 기록 조회 중 오류 발생: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "오늘 근태 기록 조회 중 오류 발생");
        }
    }

    /**
     * 특정 사용자의 월별 개인 근태 통계를 조회하는 API 엔드포인트입니다.
     *
     * @param userInfo 인증된 사용자 정보
     * @param year     조회할 연도
     * @param month    조회할 월
     * @return 개인 근태 통계 DTO 응답
     */
    @GetMapping("/personal-stats/{year}/{month}")
    public ResponseEntity<CommonResDto<PersonalAttendanceStatsDto>> getPersonalAttendanceStats(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable int year,
            @PathVariable int month) {
        try {
            PersonalAttendanceStatsDto stats = attendanceService.getPersonalAttendanceStats(userInfo.getEmployeeNo(), year, month);
            return buildSuccessResponse(stats, "개인 근태 통계 조회 성공");
        } catch (Exception e) {
            log.error("개인 근태 통계 조회 중 오류 발생: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "개인 근태 통계 조회 중 오류 발생");
        }
    }

    /**
     * 특정 사용자의 연차 현황을 조회하는 API 엔드포인트입니다.
     *
     * @param userInfo 인증된 사용자 정보
     * @return 연차 현황 정보를 담은 응답 (CommonResDto)
     */
    @GetMapping("/vacation/personal-balance")
    public ResponseEntity<CommonResDto<com.playdata.attendanceservice.client.dto.VacationBalanceResDto>> getPersonalVacationBalance(@AuthenticationPrincipal TokenUserInfo userInfo) {
        try {
            CommonResDto<com.playdata.attendanceservice.client.dto.VacationBalanceResDto> vacationBalance = attendanceService.getPersonalVacationBalance(userInfo.getEmployeeNo());
            return buildSuccessResponse(vacationBalance.getResult(), "개인 연차 현황 조회 성공");
        } catch (Exception e) {
            log.error("개인 연차 현황 조회 중 오류 발생: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "개인 연차 현황 조회 중 오류 발생");
        }
    }

    /**
     * 반차를 신청하는 API 엔드포인트입니다.
     *
     * @param userInfo 인증된 사용자 정보
     * @param requestDto 반차 신청 정보
     * @return 반차 신청 결과
     */
    @PostMapping("/half-day")
    public ResponseEntity<CommonResDto<Void>> requestHalfDay(@AuthenticationPrincipal TokenUserInfo userInfo, @RequestBody com.playdata.attendanceservice.client.dto.VacationRequestDto requestDto) {
        try {
            attendanceService.requestHalfDayVacation(userInfo.getEmployeeNo(), requestDto);
            return buildSuccessResponse(null, "반차 신청 성공");
        } catch (Exception e) {
            log.error("반차 신청 중 오류 발생: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "반차 신청 중 오류 발생");
        }
    }

    /**
     * 특정 사용자의 월별 반차 내역을 조회하는 API 엔드포인트입니다.
     *
     * @param userInfo 인증된 사용자 정보
     * @param year     조회할 연도
     * @param month    조회할 월
     * @return 월별 반차 내역 목록
     */
    @GetMapping("/personal-half-day/{year}/{month}")
    public ResponseEntity<CommonResDto<List<com.playdata.attendanceservice.client.dto.Vacation>>> getPersonalMonthlyHalfDayVacations(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable int year,
            @PathVariable int month) {
        try {
            CommonResDto<List<com.playdata.attendanceservice.client.dto.Vacation>> halfDayVacations = attendanceService.getPersonalMonthlyHalfDayVacations(userInfo.getEmployeeNo(), year, month);
            return buildSuccessResponse(halfDayVacations.getResult(), "월별 반차 내역 조회 성공");
        } catch (Exception e) {
            log.error("월별 반차 내역 조회 중 오류 발생: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "월별 반차 내역 조회 중 오류 발생");
        }
    }

    /**
     * 지각 기준 시간을 조회하는 API 엔드포인트입니다.
     *
     * @return 지각 기준 시간 문자열
     */
    @GetMapping("/late-threshold")
    public ResponseEntity<CommonResDto<String>> getLateThreshold() {
        try {
            String lateThreshold = attendanceService.getLateThreshold();
            return buildSuccessResponse(lateThreshold, "지각 기준 시간 조회 성공");
        } catch (Exception e) {
            log.error("지각 기준 시간 조회 중 오류 발생: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "지각 기준 시간 조회 중 오류 발생");
        }
    }

    /**
     * API 요청 성공 시 공통 응답 객체를 생성하여 반환하는 헬퍼 메소드입니다.
     * 이 메소드는 컨트롤러의 다른 메소드들에서 반복적으로 사용되는 성공 응답 생성을 표준화하고,
     * 코드 중복을 줄여 유지보수성을 향상시킵니다.
     *
     * @param data API 응답의 'result' 필드에 포함될 데이터입니다.
     *             제네릭 타입 <T>를 사용하여 다양한 종류의 데이터를 처리할 수 있습니다.
     *             (예: Attendance 객체, List<Attendance> 등)
     * @param message 응답 상태 메시지입니다. (예: "출근 기록 성공")
     * @return 생성된 ResponseEntity<CommonResDto<?>> 객체. HTTP 상태 코드는 200 OK로 고정됩니다.
     * @param <T> 응답 데이터의 타입
     */
    private <T> ResponseEntity<CommonResDto<T>> buildSuccessResponse(T data, String message) {
        CommonResDto<T> resDto = new CommonResDto<>(HttpStatus.OK, message, data);
        return ResponseEntity.ok(resDto);
    }

    private <T> ResponseEntity<CommonResDto<T>> buildErrorResponse(HttpStatus status, String message) {
        CommonResDto<T> resDto = new CommonResDto<>(status, message, null);
        return ResponseEntity.status(status).body(resDto);
    }
}
