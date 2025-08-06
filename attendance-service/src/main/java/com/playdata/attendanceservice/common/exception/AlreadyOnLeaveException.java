package com.playdata.attendanceservice.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST) // 400 Bad Request
public class AlreadyOnLeaveException extends AttendanceException {
    public AlreadyOnLeaveException(String message) {
        super(message);
    }
}
