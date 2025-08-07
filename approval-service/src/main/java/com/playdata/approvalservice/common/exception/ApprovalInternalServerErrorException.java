package com.playdata.approvalservice.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ApprovalInternalServerErrorException extends RuntimeException {
    public ApprovalInternalServerErrorException(String message) {
        super(message);
    }

    public ApprovalInternalServerErrorException(String message, Throwable cause) {
        super(message, cause);
    }
}
