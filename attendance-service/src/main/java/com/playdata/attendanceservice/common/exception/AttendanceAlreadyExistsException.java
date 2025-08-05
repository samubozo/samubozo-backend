package com.playdata.attendanceservice.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // 409 Conflict
public class AttendanceAlreadyExistsException extends AttendanceException {
    public AttendanceAlreadyExistsException(String message) {
        super(message);
    }

    public AttendanceAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}