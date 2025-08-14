package com.playdata.attendanceservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing; // 추가
import org.springframework.scheduling.annotation.EnableScheduling;
import com.playdata.attendanceservice.client.FeignErrorDecoder;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients // Feign 클라이언트를 활성화합니다。
@EnableJpaAuditing // JPA Auditing을 활성화합니다。
@EnableDiscoveryClient
public class AttendanceServiceApplication {

    @Bean
    public FeignErrorDecoder feignErrorDecoder() {
        return new FeignErrorDecoder();
    }

    public static void main(String[] args) {
        SpringApplication.run(AttendanceServiceApplication.class, args);
    }

}

