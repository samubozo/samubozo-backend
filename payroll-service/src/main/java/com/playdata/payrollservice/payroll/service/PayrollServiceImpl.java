package com.playdata.payrollservice.payroll.service;

import com.playdata.payrollservice.payroll.dto.PayrollRequestDto;
import com.playdata.payrollservice.payroll.dto.PayrollResponseDto;
import com.playdata.payrollservice.payroll.entity.Payroll;
import com.playdata.payrollservice.payroll.repository.PayrollRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollServiceImpl implements PayrollService {

    private final PayrollRepository payrollRepository;

    @Override
    public PayrollResponseDto savePayroll(PayrollRequestDto requestDto) {
        Long userId = requestDto.getUserId();
        int payYear = requestDto.getPayYear();
        int payMonth = requestDto.getPayMonth();

        Optional<Payroll> existing = payrollRepository.findByUserIdAndPayYearAndPayMonth(
                userId, payYear, payMonth
        );

        Payroll payroll;

        if (existing.isPresent()) {
            // ✅ 이미 존재 → 수정
            payroll = existing.get();

            if (requestDto.getBasePayroll() != null)
                payroll.setBasePayroll(requestDto.getBasePayroll());
            if (requestDto.getPositionAllowance() != null)
                payroll.setPositionAllowance(requestDto.getPositionAllowance());
            if (requestDto.getMealAllowance() != null)
                payroll.setMealAllowance(requestDto.getMealAllowance());
            if (requestDto.getBonus() != null)
                payroll.setBonus(requestDto.getBonus());
        } else {
            // ✅ 존재하지 않으면 새로 등록
            payroll = Payroll.builder()
                    .userId(userId)
                    .payYear(payYear)
                    .payMonth(payMonth)
                    .basePayroll(requestDto.getBasePayroll())
                    .positionAllowance(requestDto.getPositionAllowance())
                    .mealAllowance(requestDto.getMealAllowance())
                    .bonus(requestDto.getBonus())
                    .build();
        }

        Payroll saved = payrollRepository.save(payroll);
        return toDto(saved);
    }

    // 2. 급여 정보 조회 (userId로)
    @Override
    public PayrollResponseDto getPayrollByUserId(Long userid) {
        Payroll payroll = payrollRepository.findByUserId(userid)
                .orElseThrow(() -> new IllegalArgumentException("해당 직원의 급여 정보가 없습니다."));
//        SalaryResponseDto responseDto = toDto(salary);
//        return responseDto;
        log.info("payroll:{}", payroll);
        return toDto(payroll);
    }


    // 3. 급여 정보 수정
    @Override
    public PayrollResponseDto updatePayroll(PayrollRequestDto requestDto) {
        Payroll payroll = payrollRepository.findByUserIdAndPayYearAndPayMonth(requestDto.getUserId(),
                        requestDto.getPayYear(), requestDto.getPayMonth())
                .orElseThrow(() -> new IllegalArgumentException("수정할 급여 정보가 없습니다."));

        if (requestDto.getBasePayroll() != null)
            payroll.setBasePayroll(requestDto.getBasePayroll());
        if (requestDto.getPositionAllowance() != null)
            payroll.setPositionAllowance(requestDto.getPositionAllowance());
        if (requestDto.getMealAllowance() != null)
            payroll.setMealAllowance(requestDto.getMealAllowance());
        if (requestDto.getBonus() != null)
            payroll.setBonus(requestDto.getBonus());

        Payroll updated = payrollRepository.save(payroll);
        return toDto(updated);
    }

    // 4. 급여 정보 삭제
    @Override
    public void deletePayroll(Long userId, int payYear, int payMonth) {
        Payroll payroll = payrollRepository.findByUserIdAndPayYearAndPayMonth(userId, payYear, payMonth)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 급여 정보가 없습니다."));
        payrollRepository.delete(payroll);
    }

    // 특정 연 /월 급여 조회
    @Override
    public PayrollResponseDto getPayrollByMonth(Long userId, int year, int month) {
        Payroll payroll = payrollRepository.findByUserIdAndPayYearAndPayMonth(userId, year, month)
                .orElseThrow(() -> new IllegalArgumentException("해당 월의 급여 정보가 존재하지 않습니다."));
        return toDto(payroll);
    }

    // Entity -> Dto 변환 메서드
    private PayrollResponseDto toDto(Payroll payroll) {
        return PayrollResponseDto.builder()
                .payrollId(payroll.getPayrollId())
                .userId(payroll.getUserId())
                .basePayroll(payroll.getBasePayroll())
                .positionAllowance(payroll.getPositionAllowance())
                .bonus(payroll.getBonus())
                .payYear(payroll.getPayYear())
                .payMonth(payroll.getPayMonth())
                .mealAllowance(payroll.getMealAllowance())
                .updatedAt(payroll.getUpdatedAt())
                .build();
    }
}
