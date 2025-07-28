package com.playdata.payrollservice.common.configs;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@RequiredArgsConstructor
public class FeignClientConfig {

    @Bean
    public RequestInterceptor userHeaderForwardingInterceptor() {
        return template -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return;

            HttpServletRequest request = attributes.getRequest();
            copyHeader(request, template, "X-User-Email");
            copyHeader(request, template, "X-User-Employee-No");
            copyHeader(request, template, "X-User-Role");
        };
    }

    private void copyHeader(HttpServletRequest request, RequestTemplate template, String headerName) {
        String value = request.getHeader(headerName);
        if (value != null) {
            template.header(headerName, value);
        }
    }
}
