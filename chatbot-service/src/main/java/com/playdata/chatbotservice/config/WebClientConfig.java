// WebClientConfig.java (새 파일 생성)
package com.playdata.chatbotservice.config; // 패키지명은 적절히 조정

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Value("${ai.api.url}")
    private String aiApiUrl; // @Configuration 클래스에서는 @Value가 먼저 주입됨

    @Bean // WebClient 빈을 등록
    public WebClient geminiWebClient() {
        // aiApiUrl이 이곳에서는 이미 주입된 상태
        return WebClient.builder()
                .baseUrl(aiApiUrl)
                .build();
    }
}