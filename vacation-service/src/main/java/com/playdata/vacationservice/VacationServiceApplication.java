package com.playdata.vacationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients; // 추가
import org.springframework.data.jpa.repository.config.EnableJpaAuditing; // 추가
import org.springframework.scheduling.annotation.EnableScheduling; // 추가 (스케줄러가 있다면)

@SpringBootApplication
@EnableFeignClients // Feign 클라이언트를 활성화합니다。
@EnableJpaAuditing // JPA Auditing을 활성화합니다。
@EnableScheduling // 스케줄러가 있다면 활성화합니다。 (LeaveAccrualScheduler가 이동될 예정이므로 필요)
public class VacationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VacationServiceApplication.class, args);
    }

}
