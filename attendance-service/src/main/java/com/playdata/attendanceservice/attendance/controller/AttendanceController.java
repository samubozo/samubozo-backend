package com.playdata.attendanceservice.attendance.controller;

import com.playdata.attendanceservice.attendance.dto.AttendanceResDto;
import com.playdata.attendanceservice.attendance.dto.WorkTimeDto;
import com.playdata.attendanceservice.attendance.entity.Attendance;
import com.playdata.attendanceservice.attendance.service.AttendanceService;
import com.playdata.attendanceservice.client.VacationServiceClient; // 추가
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
import java.util.Optional; // 추가

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
@Slf4j
@RefreshScope // spring cloud config가 관리하는 파일의 데이터가 변경되면 빈들을 새로고침해주는 어노테이션
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final VacationServiceClient vacationServiceClient; // 변경

    /**
     * 사용자의 출근을 기록하는 API 엔드포인트입니다.
     *
     * @param request HttpServletRequest 객체 (클라이언트 IP 주소 획득용)
     * @return 출근 기록 성공 또는 실패에 대한 응답 (CommonResDto)
     */
    @PostMapping("/check-in")
    public ResponseEntity<CommonResDto<?>> checkIn(@AuthenticationPrincipal TokenUserInfo userInfo, HttpServletRequest request) {
        try {
            String ipAddress = request.getRemoteAddr(); // 클라이언트의 IP 주소를 가져옵니다.

            // AttendanceService의 recordCheckIn 메소드를 호출하여 출근 기록 비즈니스 로직을 수행합니다.
            Attendance attendance = attendanceService.recordCheckIn(userInfo.getEmployeeNo(), ipAddress);
            // 성공 시, CommonResDto를 생성하여 HTTP 200 OK 응답과 함께 반환합니다.
            return buildSuccessResponse(attendance, "출근 기록 성공");
        } catch (IllegalStateException e) {
            // AttendanceService에서 이미 출근 기록이 존재할 경우 던지는 IllegalStateException을 처리합니다.
            // 이 경우 HTTP 400 Bad Request 상태 코드와 함께 오류 메시지를 반환합니다.
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            // 그 외 예상치 못한 모든 예외를 처리합니다.
            // 이 경우 HTTP 500 Internal Server Error 상태 코드와 함께 일반적인 오류 메시지를 반환합니다.
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "출근 기록 중 오류 발생");
        }
    }

    /**
     * 사용자의 퇴근을 기록하는 API 엔드포인트입니다.
     *
     * @return 퇴근 기록 성공 또는 실패에 대한 응답 (CommonResDto)
     */
    @PostMapping("/check-out")
    public ResponseEntity<CommonResDto<?>> checkOut(@AuthenticationPrincipal TokenUserInfo userInfo) {
        try {
            // AttendanceService의 recordCheckOut 메소드를 호출하여 퇴근 기록 비즈니스 로직을 수행합니다.
            Attendance attendance = attendanceService.recordCheckOut(userInfo.getEmployeeNo());
            // 성공 시, CommonResDto를 생성하여 HTTP 200 OK 응답과 함께 반환합니다.
            return buildSuccessResponse(attendance, "퇴근 기록 성공");
        } catch (IllegalArgumentException e) {
            // AttendanceService에서 출근 기록이 없거나 이미 퇴근 기록이 된 경우 던지는 IllegalArgumentException을 처리합니다.
            // 이 경우 HTTP 400 Bad Request 상태 코드와 함께 오류 메시지를 반환합니다.
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            // 그 외 예상치 못한 모든 예외를 처리합니다.
            // 이 경우 HTTP 500 Internal Server Error 상태 코드와 함께 일반적인 오류 메시지를 반환합니다.
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
    public ResponseEntity<CommonResDto<?>> getMonthlyAttendance(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable int year,
            @PathVariable int month) {
        try {
            // AttendanceService의 getMonthlyAttendances 메소드를 호출하여 월별 근태 기록을 조회합니다.
            List<Attendance> monthlyAttendances = attendanceService.getMonthlyAttendances(userInfo.getEmployeeNo(), year, month);
            // 성공 시, CommonResDto를 생성하여 HTTP 200 OK 응답과 함께 조회된 데이터를 반환합니다.
            return buildSuccessResponse(monthlyAttendances, "월별 근태 조회 성공");
        } catch (Exception e) {
            // 조회 중 예상치 못한 모든 예외를 처리합니다.
            // 이 경우 HTTP 500 Internal Server Error 상태 코드와 함께 일반적인 오류 메시지를 반환합니다.
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "월별 근태 조회 중 오류 발생");
        }
    }

    @PutMapping("/go-out")
    public ResponseEntity<CommonResDto<?>> goOut(@AuthenticationPrincipal TokenUserInfo userInfo) {
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
    public ResponseEntity<CommonResDto<?>> returnFromGoOut(@AuthenticationPrincipal TokenUserInfo userInfo) {
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
    public ResponseEntity<CommonResDto<?>> getRemainingWorkTime(@AuthenticationPrincipal TokenUserInfo userInfo) {
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
    public ResponseEntity<CommonResDto<?>> getTodayAttendance(@AuthenticationPrincipal TokenUserInfo userInfo) {
        try {
            Optional<AttendanceResDto> todayAttendance = attendanceService.getTodayAttendance(userInfo.getEmployeeNo());
            if (todayAttendance.isPresent()) {
                return buildSuccessResponse(todayAttendance.get(), "오늘 근태 기록 조회 성공");
            } else {
                return buildSuccessResponse(null, "오늘 근태 기록 없음"); // 기록이 없는 경우 null 반환
            }
        } catch (Exception e) {
            log.error("오늘 근태 기록 조회 중 오류 발생: {}", e.getMessage(), e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "오늘 근태 기록 조회 중 오류 발생");
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
    private <T> ResponseEntity<CommonResDto<?>> buildSuccessResponse(T data, String message) {
        // CommonResDto 객체를 생성합니다.
        // HttpStatus.OK (200) 상태 코드, 제공된 메시지, 그리고 데이터를 사용하여 초기화합니다.
        CommonResDto<?> resDto = new CommonResDto<>(HttpStatus.OK, message, data);
        // 생성된 CommonResDto 객체를 body에 담아 ResponseEntity로 감싸 반환합니다.
        return ResponseEntity.ok(resDto);
    }

    /**
     * API 요청 실패 시 공통 응답 객체를 생성하여 반환하는 헬퍼 메소드입니다.
     * 이 메소드는 예외 발생 시 일관된 형식의 오류 응답을 생성하는 데 사용됩니다.
     *
     * @param status HTTP 응답 상태 코드입니다. (예: HttpStatus.BAD_REQUEST, HttpStatus.INTERNAL_SERVER_ERROR)
     * @param message 오류 메시지입니다. 이 메시지는 로그에도 기록됩니다.
     * @return 생성된 ResponseEntity<CommonResDto<?>> 객체.
     *         오류 응답이므로 'result' 필드는 항상 null입니다.
     */
    private ResponseEntity<CommonResDto<?>> buildErrorResponse(HttpStatus status, String message) {
        // 오류 메시지를 로그에 기록합니다.
        // log.error()를 사용하면 심각도 'ERROR'로 로그가 출력되어 문제 추적에 용이합니다.
        log.error(message);
        // CommonResDto 객체를 생성합니다.
        // 제공된 HTTP 상태 코드, 오류 메시지를 사용하고, 데이터(result)는 null로 설정합니다.
        CommonResDto<?> resDto = new CommonResDto<>(status, message, null);
        // ResponseEntity.status()를 사용하여 지정된 HTTP 상태 코드로 응답을 생성하고,
        // 생성된 CommonResDto 객체를 body에 담아 반환합니다.
        return ResponseEntity.status(status).body(resDto);
    }
}