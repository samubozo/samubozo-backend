package com.playdata.attendanceservice.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // 409 Conflict
public class AlreadyCheckedOutException extends AttendanceException {
    public AlreadyCheckedOutException(String message) {
        super(message);
    }
}
