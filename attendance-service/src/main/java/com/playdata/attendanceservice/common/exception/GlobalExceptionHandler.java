package com.playdata.attendanceservice.common.exception;

import com.playdata.attendanceservice.common.dto.CommonResDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import jakarta.persistence.EntityNotFoundException; // import 추가
import org.springframework.security.access.AccessDeniedException; // import 추가 (AuthorizationDeniedException 대신 AccessDeniedException 사용)

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * AttendanceAlreadyExistsException 처리: 이미 출근 기록이 존재하는 경우 발생
     * HTTP Status: 409 Conflict
     */
    @ExceptionHandler(AttendanceAlreadyExistsException.class)
    public ResponseEntity<CommonResDto<?>> handleAttendanceAlreadyExistsException(AttendanceAlreadyExistsException ex) {
        log.warn("AttendanceAlreadyExistsException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage());
    }

    /**
     * CheckInNotFoundException 처리: 출근 기록을 찾을 수 없는 경우 발생
     * HTTP Status: 404 Not Found
     */
    @ExceptionHandler(CheckInNotFoundException.class)
    public ResponseEntity<CommonResDto<?>> handleCheckInNotFoundException(CheckInNotFoundException ex) {
        log.warn("CheckInNotFoundException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * AlreadyCheckedOutException 처리: 이미 퇴근 기록이 완료된 경우 발생
     * HTTP Status: 400 Bad Request
     */
    @ExceptionHandler(AlreadyCheckedOutException.class)
    public ResponseEntity<CommonResDto<?>> handleAlreadyCheckedOutException(AlreadyCheckedOutException ex) {
        log.warn("AlreadyCheckedOutException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * AlreadyOnLeaveException 처리: 이미 외출/복귀 처리된 경우 발생
     * HTTP Status: 400 Bad Request
     */
    @ExceptionHandler(AlreadyOnLeaveException.class)
    public ResponseEntity<CommonResDto<?>> handleAlreadyOnLeaveException(AlreadyOnLeaveException ex) {
        log.warn("AlreadyOnLeaveException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * IllegalArgumentException 처리: 잘못된 인자 값이 전달된 경우 발생
     * HTTP Status: 400 Bad Request
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonResDto<?>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * IllegalStateException 처리: 객체의 상태가 유효하지 않은 경우 발생
     * HTTP Status: 400 Bad Request
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<CommonResDto<?>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("IllegalStateException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * EntityNotFoundException 처리: 엔티티를 찾을 수 없는 경우 발생
     * HTTP Status: 404 Not Found
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<CommonResDto<?>> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.warn("EntityNotFoundException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NO_CONTENT, ex.getMessage());
    }

    /**
     * AccessDeniedException 처리: 권한이 없는 경우 발생 (Spring Security)
     * HTTP Status: 403 Forbidden
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CommonResDto<?>> handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("AccessDeniedException: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, "접근 권한이 없습니다.");
    }

    /**
     * 모든 예상치 못한 예외 처리: 일반적인 서버 오류
     * HTTP Status: 500 Internal Server Error
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResDto<?>> handleAllUncaughtException(Exception ex) {
        log.error("Uncaught Exception: {}", ex.getMessage(), ex);
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");
    }

    /**
     * 공통 오류 응답 객체를 생성하여 반환하는 헬퍼 메소드입니다.
     */
    private ResponseEntity<CommonResDto<?>> buildErrorResponse(HttpStatus status, String message) {
        CommonResDto<?> resDto = new CommonResDto<>(status, message, null);
        return ResponseEntity.status(status).body(resDto);
    }
}