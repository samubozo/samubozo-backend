package com.playdata.approvalservice.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ApprovalForbiddenException extends RuntimeException {
    public ApprovalForbiddenException(String message) {
        super(message);
    }
}
