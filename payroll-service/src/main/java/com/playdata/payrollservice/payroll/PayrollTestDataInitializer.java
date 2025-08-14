package com.playdata.payrollservice.payroll;

import com.playdata.payrollservice.payroll.entity.Payroll;
import com.playdata.payrollservice.payroll.repository.PayrollRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PayrollTestDataInitializer implements CommandLineRunner {

    private final PayrollRepository payrollRepository;

    @Override
    public void run(String... args) {
        // userId 1
        createPayroll(1L, 2025, 8, null, 350000, 280000, 300000);
        createPayroll(1L, 2025, 7, null, 350000, 280000);
        createPayroll(1L, 2025, 6, null, 300000, 280000);
        createPayroll(1L, 2025, 5, 7700000, 300000, 280000);

        // userId 2
        createPayroll(2L, 2025, 8, null, 230000, 260000, 1000000);
        createPayroll(2L, 2025, 7, null, 230000, 260000, 1000000);
        createPayroll(2L, 2025, 6, null, 200000, 260000);
        createPayroll(2L, 2025, 5, 7500000, 200000, 260000);

        // userId 3
        createPayroll(3L, 2025, 8, null, 120000, 260000);
        createPayroll(3L, 2025, 7, 5500000, 120000, 260000);
        createPayroll(3L, 2025, 6, 5400000, 100000, 260000);
        createPayroll(3L, 2025, 5, 5400000, 100000, 260000);

        // userId 4
        createPayroll(4L, 2025, 8, null, 150000, 260000);
        createPayroll(4L, 2025, 7, 3500000, 150000, 260000);
        createPayroll(4L, 2025, 6, 3400000, 160000, 260000, 200000);
        createPayroll(4L, 2025, 5, 3400000, 160000, 260000, 200000);

        // userId 5
        createPayroll(5L, 2025, 8, null, 120000, 260000);
        createPayroll(5L, 2025, 7, 2700000, 120000, 260000);
        createPayroll(5L, 2025, 6, 2500000, 100000, 260000, 300000);
        createPayroll(5L, 2025, 5, 2500000, 100000, 260000, 300000);
    }


    // bonus 없는 버전
    private void createPayroll(Long userId, int year, int month, Integer base, int allowance, Integer meal) {
        createPayroll(userId, year, month, base, allowance, meal, null); // bonus를 null로 처리
    }

    // bonus 있는 버전
    private void createPayroll(Long userId, int year, int month, Integer base, int allowance, int meal, Integer bonus) {
        boolean exists = payrollRepository.findByUserIdAndPayYearAndPayMonth(userId, year, month).isPresent();
        if (!exists) {
            Payroll payroll = Payroll.builder()
                    .userId(userId)
                    .payYear(year)
                    .payMonth(month)
                    .positionAllowance(allowance)
                    .mealAllowance(meal)
                    .bonus(bonus)
                    .build();

            payrollRepository.save(payroll);
            log.info("✅ 급여 더미 데이터 저장됨: userId={}, {}/{}", userId, year, month);
        } else {
            log.info("⚠️ 이미 존재하는 급여: userId={}, {}/{} → 스킵", userId, year, month);
        }
    }
}
