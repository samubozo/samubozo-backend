package com.playdata.scheduleservice.client;

import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("{} request failed with status {}. Reason: {}", methodKey, response.status(), response.reason());

        if (response.status() == HttpStatus.NOT_FOUND.value()) {
            if (methodKey.contains("getUserByEmployeeNo")) {
                return new ResponseStatusException(HttpStatus.NOT_FOUND, "HR Service에서 사용자를 찾을 수 없습니다.");
            }
        } else if (response.status() == HttpStatus.BAD_REQUEST.value()) {
            return new ResponseStatusException(HttpStatus.BAD_REQUEST, "HR Service 요청이 잘못되었습니다.");
        } else if (response.status() == HttpStatus.UNAUTHORIZED.value()) {
            return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "HR Service 인증에 실패했습니다.");
        }

        return new ResponseStatusException(HttpStatus.valueOf(response.status()), response.reason());
    }
}
