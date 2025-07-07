package com.playdata.attendanceservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
// import org.springframework.data.jpa.repository.config.EnableJpaAuditing; // 제거
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableFeignClients // Feign 클라이언트를 활성화합니다。
// @EnableJpaAuditing // JPA Auditing을 활성화합니다。 (제거)
public class AttendanceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AttendanceServiceApplication.class, args);
    }

}

