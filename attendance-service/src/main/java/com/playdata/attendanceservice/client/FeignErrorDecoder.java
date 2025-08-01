package com.playdata.attendanceservice.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import feign.FeignException;

@Component
@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus status = HttpStatus.resolve(response.status());
        String errorMessage = String.format("Feign Client 호출 오류: %s, Status: %d, Reason: %s",
                methodKey, response.status(), response.reason());

        log.error(errorMessage);

        if (status != null && status.is4xxClientError()) {
            // 4xx 클라이언트 에러는 BAD_REQUEST로 처리
            return new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMessage);
        } else if (status != null && status.is5xxServerError()) {
            // 5xx 서버 에러는 INTERNAL_SERVER_ERROR로 처리
            return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, errorMessage);
        }
        // 그 외의 경우 기본 FeignException 반환
        byte[] body = null;
        try {
            if (response.body() != null) {
                body = response.body().asInputStream().readAllBytes();
            }
        } catch (Exception e) {
            log.error("Failed to read response body for FeignException: {}", e.getMessage());
        }
        return new FeignException.FeignClientException(response.status(), response.reason(), response.request(), body, response.headers());
    }
}
