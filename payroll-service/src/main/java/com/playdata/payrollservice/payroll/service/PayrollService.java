package com.playdata.payrollservice.payroll.service;

import com.playdata.payrollservice.payroll.dto.PayrollRequestDto;
import com.playdata.payrollservice.payroll.dto.PayrollResponseDto;
import com.playdata.payrollservice.payroll.entity.Payroll;
import com.playdata.payrollservice.payroll.repository.PayrollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PayrollService {

    private final PayrollRepository payrollRepository;

    public PayrollResponseDto savePayroll(PayrollRequestDto requestDto) {

        // 1. 급여 정보 저장
        Payroll payroll = Payroll.builder()
                .userId(requestDto.getUserId())
                .basePayroll(requestDto.getBasePayroll())
                .positionAllowance(requestDto.getPositionAllowance())
                .mealAllowance(requestDto.getMealAllowance())
                .build();

        Payroll saved = payrollRepository.save(payroll);
        return toDto(saved);

    }

    // 2. 급여 정보 조회 (userId로)
    public PayrollResponseDto getPayrollByUserId(Long userid) {
        Payroll payroll = payrollRepository.findByUserId(userid)
                .orElseThrow(() -> new IllegalArgumentException("해당 직원의 급여 정보가 없습니다."));
//        SalaryResponseDto responseDto = toDto(salary);
//        return responseDto;
        return toDto(payroll);
    }


    // 3. 급여 정보 수정
    public PayrollResponseDto updatePayroll(Long userId, PayrollRequestDto requestDto) {
        Payroll payroll = payrollRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("수정할 급여 정보가 없습니다."));

        payroll.setBasePayroll(requestDto.getBasePayroll());
        payroll.setPositionAllowance(requestDto.getPositionAllowance());
        payroll.setMealAllowance(requestDto.getMealAllowance());

        Payroll updated = payrollRepository.save(payroll);
        return toDto(updated);
    }


    // Entity -> Dto 변환 메서드
    private PayrollResponseDto toDto(Payroll payroll) {
        return PayrollResponseDto.builder()
                .payrollId(payroll.getPayrollId())
                .userId(payroll.getUserId())
                .basePayroll(payroll.getBasePayroll())
                .positionAllowance(payroll.getPositionAllowance())
                .mealAllowance(payroll.getMealAllowance())
                .updatedAt(payroll.getUpdatedAt())
                .build();
    }
}
