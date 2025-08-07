package com.playdata.attendanceservice.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND) // 404 Not Found
public class CheckInNotFoundException extends AttendanceException {
    public CheckInNotFoundException(String message) {
        super(message);
    }
}
