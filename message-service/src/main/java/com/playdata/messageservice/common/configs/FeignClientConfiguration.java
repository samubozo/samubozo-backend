package com.playdata.messageservice.common.configs;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Objects;

@Configuration
public class FeignClientConfiguration {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (Objects.nonNull(attributes)) {
                HttpServletRequest request = attributes.getRequest();
                // X-Authentication-Id 헤더 추가
                String employeeNo = request.getHeader("X-Authentication-Id");
                if (Objects.nonNull(employeeNo) && !employeeNo.isEmpty()) {
                    requestTemplate.header("X-Authentication-Id", employeeNo);
                }
                // Authorization 헤더도 함께 전달 (선택 사항, 필요에 따라)
                String authorizationHeader = request.getHeader("Authorization");
                if (Objects.nonNull(authorizationHeader) && !authorizationHeader.isEmpty()) {
                    requestTemplate.header("Authorization", authorizationHeader);
                }
            }
        };
    }
}
