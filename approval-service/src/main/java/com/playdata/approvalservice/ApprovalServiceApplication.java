package com.playdata.approvalservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing; // 이 import 추가
import org.springframework.core.env.Environment;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients // Feign 클라이언트를 활성화합니다。
@EnableJpaAuditing // 이 어노테이션 추가
@EnableCaching
@EnableDiscoveryClient
@Slf4j
public class ApprovalServiceApplication {

    @Autowired
    private Environment env;

    @PostConstruct
    public void logDdlAutoAndDbUrl() {
        log.info("spring.jpa.hibernate.ddl-auto: {}", env.getProperty("spring.jpa.hibernate.ddl-auto"));
        log.info("spring.datasource.url: {}", env.getProperty("spring.datasource.url"));
    }

    public static void main(String[] args) {
        SpringApplication.run(ApprovalServiceApplication.class, args);
    }

}
