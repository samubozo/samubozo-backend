package com.playdata.attendanceservice.common.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.RequestInterceptor;
import feign.codec.Encoder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.SpringEncoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@Slf4j
public class FeignClientConfig {

    @Bean
    public Encoder feignEncoder() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        MappingJackson2HttpMessageConverter jacksonConverter = new MappingJackson2HttpMessageConverter(objectMapper);
        ObjectFactory<HttpMessageConverters> converters = () -> new HttpMessageConverters(jacksonConverter);

        return new SpringEncoder(converters);
    }

    // Feign 요청을 가로채서 헤더를 추가하는 인터셉터 빈을 정의합니다.
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // 현재 HTTP 요청의 컨텍스트를 가져옵니다.
            // 이 컨텍스트에는 Gateway로부터 전달받은 X-User-* 헤더가 포함되어 있습니다.
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest httpRequest = attributes.getRequest();

                // Gateway에서 주입한 사용자 정보 헤더들을 가져와 Feign 요청에 추가합니다.
                // 이 헤더들은 vacation-service의 JwtAuthFilter에서 사용될 것입니다.
                String userEmail = httpRequest.getHeader("X-User-Email");
                String userRole = httpRequest.getHeader("X-User-Role");
                String employeeNo = httpRequest.getHeader("X-User-Employee-No");

                // 각 헤더가 null이 아닌 경우에만 Feign 요청에 추가합니다.
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
                // RequestContextHolder가 null인 경우는 주로 비동기 스레드에서 호출되거나,
                // HTTP 요청 컨텍스트 외부에서 Feign 클라이언트가 호출될 때 발생할 수 있습니다.
                // 이 경우에는 헤더를 전파할 수 없으므로 경고를 로깅합니다.
                log.warn("Feign Interceptor: RequestContextHolder is null. Cannot propagate X-User-* headers.");
            }
        };
    }
}