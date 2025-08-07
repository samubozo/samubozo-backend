package com.playdata.attendanceservice.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AttendanceException extends BusinessException {
    public AttendanceException(String message) {
        super(message);
    }

    public AttendanceException(String message, Throwable cause) {
        super(message, cause);
    }
}
