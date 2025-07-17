package com.playdata.payrollservice.payroll.service;

import com.playdata.payrollservice.payroll.dto.PayrollExtraDetailDto;
import com.playdata.payrollservice.payroll.dto.PayrollExtraRequestDto;
import com.playdata.payrollservice.payroll.entity.PayrollExtra;

public interface PayrollExtraService {
    // 추가 수당 저장
    PayrollExtra saveExtra(PayrollExtra extra);

    // 추가 수당 조회
    PayrollExtra getExtraById(Long id);

    // 유저 정보와 함께
    PayrollExtraDetailDto getExtraWithUser(Long id);

    // 수당 수정
    PayrollExtra updateExtra(Long id, PayrollExtraRequestDto requestDto);

    // 수당 삭제
    void deleteExtra(Long id);
}
