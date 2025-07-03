package com.playdata.hrservice.client;

import com.playdata.hrservice.hr.dto.UserLoginReqDto;
import com.playdata.hrservice.hr.dto.UserLoginResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthServiceClient {

    private final RestTemplate restTemplate;
    private static final String AUTH_SERVICE_URL = "http://auth-service/auth";

    /**
     * Auth Service를 통해 로그인을 수행합니다.
     * @param loginReqDto 로그인 요청 정보
     * @return 로그인 응답 (토큰 포함)
     */
    public UserLoginResDto login(UserLoginReqDto loginReqDto) {
        try {
            log.info("Auth Service 호출: 로그인 요청 - {}", loginReqDto.getEmail());
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                AUTH_SERVICE_URL + "/login",
                loginReqDto,
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                
                // Auth Service 응답을 HR Service 응답 형식으로 변환
                return UserLoginResDto.builder()
                    .accessToken((String) data.get("token"))
                    .refreshToken(null) // Auth Service에서 refreshToken을 별도로 저장하므로 null
                    .tokenType("Bearer")
                    .expiresIn(3600L) // 기본값, 실제로는 토큰에서 추출
                    .build();
            } else {
                throw new RuntimeException("Auth Service 로그인 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Auth Service 호출 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("로그인 서비스에 일시적인 문제가 발생했습니다.", e);
        }
    }

    /**
     * 토큰 갱신을 요청합니다.
     * @param refreshToken 리프레시 토큰
     * @return 새로운 액세스 토큰
     */
    public String refreshToken(String refreshToken) {
        try {
            log.info("Auth Service 호출: 토큰 갱신 요청");
            
            Map<String, String> request = Map.of("refreshToken", refreshToken);
            
            ResponseEntity<Map> response = restTemplate.postForEntity(
                AUTH_SERVICE_URL + "/token/refresh",
                request,
                Map.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return (String) response.getBody().get("accessToken");
            } else {
                throw new RuntimeException("토큰 갱신 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("토큰 갱신 중 오류 발생: {}", e.getMessage());
            throw new RuntimeException("토큰 갱신에 실패했습니다.", e);
        }
    }
} 