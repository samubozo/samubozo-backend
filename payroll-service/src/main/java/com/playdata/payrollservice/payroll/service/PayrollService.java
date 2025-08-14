package com.playdata.payrollservice.payroll.service;

import com.playdata.payrollservice.common.auth.TokenUserInfo;
import com.playdata.payrollservice.payroll.dto.PayrollRequestDto;
import com.playdata.payrollservice.payroll.dto.PayrollResponseDto;

public interface PayrollService {
    // 급여 정보 저장
    PayrollResponseDto savePayroll(PayrollRequestDto requestDto, TokenUserInfo userInfo);

    // 급여 정보 수정
    PayrollResponseDto updatePayroll(PayrollRequestDto requestDto);

    // 급여 정보 삭제
    void deletePayroll(Long userId, int payYear, int payMonth);

    // 특정 연 /월 급여 조회
    PayrollResponseDto getPayrollByMonth(Long userId, int year, int month);

    // 급여 자동 생성
    void generateMonthlyPayrollForAll();
}
