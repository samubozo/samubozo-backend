package com.playdata.vacationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;
import com.playdata.vacationservice.vacation.service.VacationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableFeignClients
@EnableJpaAuditing
@EnableScheduling
public class VacationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(VacationServiceApplication.class, args);
    }

    // 개발 환경에서 테스트를 위해 더미 연차 데이터를 추가하는 CommandLineRunner
    @Bean
    public CommandLineRunner initData(VacationService vacationService) {
        return args -> {
            // 테스트할 사용자 ID 목록
            Long[] testUserIds = {1L, 2L, 3L, 4L, 5L}; // 예시: 1번부터 5번까지의 사용자 ID
            int initialVacationDays = 15; // 부여할 연차 일수

            for (Long userId : testUserIds) {
                try {
                    vacationService.grantAnnualLeave(userId, initialVacationDays);
                    System.out.println("테스트 사용자(ID: " + userId + ")에게 연차 " + initialVacationDays + "일이 성공적으로 부여되었습니다.");
                } catch (Exception e) {
                    System.err.println("테스트 사용자(ID: " + userId + ") 연차 부여 중 오류 발생: " + e.getMessage());
                    // 이미 연차가 부여되어 있을 경우 예외가 발생할 수 있으므로, 무시하거나 로그만 남깁니다.
                }
            }
        };
    }

}
