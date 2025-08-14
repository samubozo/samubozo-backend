package com.playdata.approvalservice.common.configs;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@Slf4j
public class FeignClientConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest httpRequest = attributes.getRequest();

                String userEmail = httpRequest.getHeader("X-User-Email");
                String userRole = httpRequest.getHeader("X-User-Role");
                String employeeNo = httpRequest.getHeader("X-User-Employee-No");

                if (userEmail != null) {
                    requestTemplate.header("X-User-Email", userEmail);
                    log.debug("Feign Interceptor: Added X-User-Email header: {}", userEmail);
                }
                if (userRole != null) {
                    requestTemplate.header("X-User-Role", userRole);
                    log.debug("Feign Interceptor: Added X-User-Role header: {}", userRole);
                }
                if (employeeNo != null) {
                    requestTemplate.header("X-User-Employee-No", employeeNo);
                    log.debug("Feign Interceptor: Added X-User-Employee-No header: {}", employeeNo);
                }
            } else {
                log.warn("Feign Interceptor: RequestContextHolder is null. Cannot propagate X-User-* headers.");
            }
        };
    }
}
