// PayrollScheduler.java
package com.playdata.payrollservice.payroll.scheduler;

import com.playdata.payrollservice.payroll.service.PayrollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PayrollScheduler {

    private final PayrollService payrollService;
    
    @Scheduled(cron = "0 0 10 1 * ?")
    //@Scheduled(cron = "0/30 * * * * ?")  // 매 30초마다 실행
    public void generateMonthlyPayroll() {
        log.info("📌 스케줄러: 매달 25일 급여 자동 생성 시작");

        payrollService.generateMonthlyPayrollForAll();

        log.info("✅ 스케줄러: 급여 생성 완료");
    }
}
