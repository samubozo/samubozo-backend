package com.playdata.approvalservice.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ApprovalNotFoundException extends RuntimeException {
    public ApprovalNotFoundException(String message) {
        super(message);
    }
}
