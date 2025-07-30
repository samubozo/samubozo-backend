package com.playdata.payrollservice.payroll.service;

import com.playdata.payrollservice.common.auth.TokenUserInfo;
import com.playdata.payrollservice.payroll.dto.PayrollRequestDto;
import com.playdata.payrollservice.payroll.dto.PayrollResponseDto;

public interface PayrollService {
    PayrollResponseDto savePayroll(PayrollRequestDto requestDto, TokenUserInfo userInfo);

    // 2. 급여 정보 조회 (userId로)
    PayrollResponseDto getPayrollByUserId(Long userid);

    // 3. 급여 정보 수정
    PayrollResponseDto updatePayroll(PayrollRequestDto requestDto);

    // 4. 급여 정보 삭제
    void deletePayroll(Long userId, int payYear, int payMonth);

    // 특정 연 /월 급여 조회
    PayrollResponseDto getPayrollByMonth(Long userId, int year, int month);

    void generateMonthlyPayrollForAll();
}
