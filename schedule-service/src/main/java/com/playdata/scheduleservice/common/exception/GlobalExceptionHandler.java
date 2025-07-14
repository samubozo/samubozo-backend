package com.playdata.scheduleservice.common.exception;

import com.playdata.scheduleservice.common.dto.CommonResDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<CommonResDto<Void>> handleIllegalStateException(IllegalStateException ex) {
        CommonResDto<Void> errorResponse = new CommonResDto<>(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
        return ResponseEntity.ok(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonResDto<Void>> handleIllegalArgumentException(IllegalArgumentException ex) {
        CommonResDto<Void> errorResponse = new CommonResDto<>(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
        return ResponseEntity.ok(errorResponse);
    }

    // 다른 예외 유형에 대한 핸들러를 추가할 수 있습니다.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResDto<Void>> handleAllExceptions(Exception ex) {
        CommonResDto<Void> errorResponse = new CommonResDto<>(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다: " + ex.getMessage(), null);
        return ResponseEntity.ok(errorResponse);
    }
}