package com.playdata.vacationservice.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import feign.FeignException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
public class FeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, Response response) {
        HttpStatus status = HttpStatus.resolve(response.status());
        String errorMessage = String.format("Feign Client 호출 오류: %s, Status: %d, Reason: %s",
                methodKey, response.status(), response.reason());

        byte[] body = null;
        String responseBody = "";
        try {
            if (response.body() != null) {
                body = response.body().asInputStream().readAllBytes();
                responseBody = new String(body, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("Failed to read response body for FeignException: {}", e.getMessage());
        }

        log.error("{} - Response Body: {}", errorMessage, responseBody);

        String extractedMessage = extractMessageFrom(responseBody, errorMessage);

        if (status != null) {
            if (status.is4xxClientError() || status.is5xxServerError()) {
                return new ResponseStatusException(status, extractedMessage);
            }
        }

        return new FeignException.FeignClientException(response.status(), response.reason(), response.request(), body, response.headers());
    }

    private String extractMessageFrom(String json, String fallbackMessage) {
        try {
            JsonNode jsonNode = objectMapper.readTree(json);
            if (jsonNode.has("statusMessage")) {
                return jsonNode.get("statusMessage").asText();
            }
        } catch (IOException e) {
            log.error("Failed to parse response body as JSON: {}", e.getMessage());
        }
        return fallbackMessage;
    }
}
