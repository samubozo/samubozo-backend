package com.playdata.attendanceservice.attendance.controller;

import com.playdata.attendanceservice.client.dto.VacationRequestDto; // 경로 변경
import com.playdata.attendanceservice.attendance.entity.Attendance;
import com.playdata.attendanceservice.attendance.service.AttendanceService;
import com.playdata.attendanceservice.client.VacationServiceClient; // 추가
import com.playdata.attendanceservice.common.dto.CommonResDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.List;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
@Slf4j
@RefreshScope // spring cloud config가 관리하는 파일의 데이터가 변경되면 빈들을 새로고침해주는 어노테이션
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final VacationServiceClient vacationServiceClient; // 변경

    /**
     * 휴가를 신청하는 API 엔드포인트입니다.
     *
     * @param userId 신청자 ID (요청 헤더에서 추출)
     * @param requestDto 휴가 신청 정보
     * @return 휴가 신청 성공 또는 실패에 대한 응답
     */
    @PostMapping("/vacations")
    public ResponseEntity<CommonResDto<?>> requestVacation(
            @RequestHeader("X-USER-ID") Long userId,
            @RequestBody VacationRequestDto requestDto) {
        try {
            vacationServiceClient.requestVacation(userId, requestDto); // 변경
            return buildSuccessResponse(null, "휴가 신청이 성공적으로 접수되었습니다.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "휴가 신청 중 오류가 발생했습니다.");
        }
    }

    /**
     * 사용자의 출근을 기록하는 API 엔드포인트입니다.
     *
     * @param userId 출근을 기록할 사용자의 ID (URL 경로에서 추출)
     * @param request HttpServletRequest 객체 (클라이언트 IP 주소 획득용)
     * @return 출근 기록 성공 또는 실패에 대한 응답 (CommonResDto)
     */
    @PostMapping("/check-in/{userId}")
    public ResponseEntity<CommonResDto<?>> checkIn(@PathVariable Long userId, HttpServletRequest request) {
        try {
            String ipAddress = request.getRemoteAddr(); // 클라이언트의 IP 주소를 가져옵니다.

            // AttendanceService의 recordCheckIn 메소드를 호출하여 출근 기록 비즈니스 로직을 수행합니다.
            Attendance attendance = attendanceService.recordCheckIn(userId, ipAddress);
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
     * @param userId 퇴근을 기록할 사용자의 ID (URL 경로에서 추출)
     * @return 퇴근 기록 성공 또는 실패에 대한 응답 (CommonResDto)
     */
    @PostMapping("/check-out/{userId}")
    public ResponseEntity<CommonResDto<?>> checkOut(@PathVariable Long userId) {
        try {
            // AttendanceService의 recordCheckOut 메소드를 호출하여 퇴근 기록 비즈니스 로직을 수행합니다.
            Attendance attendance = attendanceService.recordCheckOut(userId);
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
     * @param userId 조회할 사용자의 ID (URL 경로에서 추출)
     * @param year 조회할 연도 (URL 경로에서 추출)
     * @param month 조회할 월 (URL 경로에서 추출)
     * @return 월별 근태 기록 목록 또는 오류에 대한 응답 (CommonResDto)
     */
    @GetMapping("/monthly/{userId}/{year}/{month}")
    public ResponseEntity<CommonResDto<?>> getMonthlyAttendance(
            @PathVariable Long userId,
            @PathVariable int year,
            @PathVariable int month) {
        try {
            // AttendanceService의 getMonthlyAttendances 메소드를 호출하여 월별 근태 기록을 조회합니다.
            List<Attendance> monthlyAttendances = attendanceService.getMonthlyAttendances(userId, year, month);
            // 성공 시, CommonResDto를 생성하여 HTTP 200 OK 응답과 함께 조회된 데이터를 반환합니다.
            return buildSuccessResponse(monthlyAttendances, "월별 근태 조회 성공");
        } catch (Exception e) {
            // 조회 중 예상치 못한 모든 예외를 처리합니다.
            // 이 경우 HTTP 500 Internal Server Error 상태 코드와 함께 일반적인 오류 메시지를 반환합니다.
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "월별 근태 조회 중 오류 발생");
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
