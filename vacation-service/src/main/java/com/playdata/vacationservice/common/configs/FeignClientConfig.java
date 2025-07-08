package com.playdata.vacationservice.common.configs;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign Client 설정을 위한 Configuration 클래스입니다.
 * FeignClientInterceptor를 빈으로 등록하여 Feign Client 요청에 적용합니다.
 */
@Configuration
public class FeignClientConfig {

    @Bean
    public RequestInterceptor feignClientInterceptor() {
        return new FeignClientInterceptor();
    }
}
