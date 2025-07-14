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
        createPayroll(1L, 2025, 7, 3400000, 200000, 120000);
        createPayroll(1L, 2025, 6, 2800000, 150000, 100000);
        createPayroll(1L, 2025, 5, 2700000, 100000, 80000);

        // userId 2
        createPayroll(2L, 2025, 7, 3100000, 180000, 110000);
        createPayroll(2L, 2025, 6, 2700000, 130000, 90000);
        createPayroll(2L, 2025, 5, 2500000, 100000, 80000);

        // userId 3
        createPayroll(3L, 2025, 7, 2900000, 160000, 105000);
        createPayroll(3L, 2025, 6, 2600000, 120000, 85000);
        createPayroll(3L, 2025, 5, 2400000, 100000, 75000);
    }

    private void createPayroll(Long userId, int year, int month, int base, int allowance, int meal) {
        boolean exists = payrollRepository.findByUserIdAndPayYearAndPayMonth(userId, year, month).isPresent();
        if (!exists) {
            Payroll payroll = Payroll.builder()
                    .userId(userId)
                    .payYear(year)
                    .payMonth(month)
                    .basePayroll(base)
                    .positionAllowance(allowance)
                    .mealAllowance(meal)
                    .build();

            payrollRepository.save(payroll);
            log.info("✅ 급여 더미 데이터 저장됨: userId={}, {}/{}", userId, year, month);
        } else {
            log.info("⚠️ 이미 존재하는 급여: userId={}, {}/{} → 스킵", userId, year, month);
        }
    }
}
