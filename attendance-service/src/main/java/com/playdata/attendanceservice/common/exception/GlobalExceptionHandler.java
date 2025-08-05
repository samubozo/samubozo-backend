package com.playdata.attendanceservice.common.exception;

import com.playdata.attendanceservice.common.dto.CommonResDto;
import feign.FeignException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Feign Client 관련 예외 처리
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<CommonResDto<Void>> handleFeignException(FeignException e) {
        // Feign 예외는 매우 복잡하므로, 간단하고 명확한 메시지를 프론트에 전달합니다.
        String message = "다른 서비스와 통신 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        if (e.status() > 0) {
            message += " (오류 코드: " + e.status() + ")";
        }
        CommonResDto<Void> response = new CommonResDto<>(HttpStatus.INTERNAL_SERVER_ERROR, message, null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // 비즈니스 로직 관련 예외 처리 (예: 잘못된 요청)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonResDto<Void>> handleIllegalArgumentException(IllegalArgumentException e) {
        CommonResDto<Void> response = new CommonResDto<>(HttpStatus.BAD_REQUEST, e.getMessage(), null);
        return ResponseEntity.badRequest().body(response);
    }

    // 그 외 모든 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResDto<Void>> handleGlobalException(Exception e) {
        CommonResDto<Void> response = new CommonResDto<>(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", null);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
