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

        // ✅ 2. 존재하면 중복 입력 방지
        if (existing.isPresent()) {
            throw new IllegalArgumentException("이미 해당 월의 급여 정보가 존재합니다.");
        }
        // 1. 급여 정보 저장
        Payroll payroll = Payroll.builder()
                .userId(requestDto.getUserId())
                .basePayroll(requestDto.getBasePayroll())
                .positionAllowance(requestDto.getPositionAllowance())
                .mealAllowance(requestDto.getMealAllowance())
                .payYear(requestDto.getPayYear())
                .payMonth(requestDto.getPayMonth())
                .build();

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
        Payroll payroll = payrollRepository.findByUserId(requestDto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("수정할 급여 정보가 없습니다."));

        if (requestDto.getBasePayroll() != null)
            payroll.setBasePayroll(requestDto.getBasePayroll());
        if (requestDto.getPositionAllowance() != null)
            payroll.setPositionAllowance(requestDto.getPositionAllowance());
        if (requestDto.getMealAllowance() != null)
            payroll.setMealAllowance(requestDto.getMealAllowance());

        Payroll updated = payrollRepository.save(payroll);
        return toDto(updated);
    }

    // 4. 급여 정보 삭제
    @Override
    public void deletePayroll(Long userId) {
        Payroll payroll = payrollRepository.findByUserId(userId)
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
                .mealAllowance(payroll.getMealAllowance())
                .updatedAt(payroll.getUpdatedAt())
                .build();
    }
}
