package com.playdata.approvalservice.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ApprovalBadRequestException extends RuntimeException {
    public ApprovalBadRequestException(String message) {
        super(message);
    }
}
