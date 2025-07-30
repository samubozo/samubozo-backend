package com.playdata.payrollservice.payroll.service;

import com.playdata.payrollservice.client.AttendanceClient;
import com.playdata.payrollservice.client.HrClient;
import com.playdata.payrollservice.common.auth.TokenUserInfo;
import com.playdata.payrollservice.common.dto.CommonResDto;
import com.playdata.payrollservice.payroll.dto.*;
import com.playdata.payrollservice.payroll.entity.Payroll;
import com.playdata.payrollservice.payroll.repository.PayrollRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollServiceImpl implements PayrollService {

    private final AttendanceClient attendanceClient;
    private final PayrollRepository payrollRepository;
    private final HrClient hrClient;

    private static final Map<String, Integer> POSITION_BASE_PAY_MAP = Map.of(
            "사장", 9000000,
            "부장", 7000000,
            "책임", 6000000,
            "선임", 4500000,
            "사원", 3000000
    );
    private static final Map<String, Integer> POSITION_ALLOWANCE_MAP = Map.of(
            "사장", 1000000,
            "부장", 800000,
            "책임", 600000,
            "선임", 400000,
            "사원", 300000
    );
    private static final Map<String, Integer> POSITION_MEAL_ALLOWANCE_MAP = Map.of(
            "사장", 300000,
            "부장", 260000,
            "책임", 260000,
            "선임", 260000,
            "사원", 260000
    );

    @Override
    public PayrollResponseDto savePayroll(PayrollRequestDto requestDto, TokenUserInfo userInfo) {

        Long userId = requestDto.getUserId();
        int payYear = requestDto.getPayYear();
        int payMonth = requestDto.getPayMonth();

        // ✅ 이미 존재하면 아무 작업도 하지 않음
        Optional<Payroll> existing = payrollRepository.findByUserIdAndPayYearAndPayMonth(userId, payYear, payMonth);
        if (existing.isPresent() && existing.get().getBasePayroll() != null && existing.get().getBasePayroll() != 0) {
            log.info("⏩ 이미 생성된 급여가 존재하여 스킵: userId={}, {}/{}", userId, payYear, payMonth);
            return toDto(existing.get());
        }

        String positionName = Optional.ofNullable(requestDto.getPositionName())
                .filter(POSITION_BASE_PAY_MAP::containsKey)
                .orElseGet(() -> getUserPosition(userId));
        int positionAllowance = Optional.ofNullable(requestDto.getPositionAllowance())
                .filter(p -> p != 0) // 0이면 무시
                .orElse(POSITION_ALLOWANCE_MAP.getOrDefault(positionName, 0)); // 직급에 맞는 수당

        int mealAllowance = Optional.ofNullable(requestDto.getMealAllowance())
                .filter(m -> m != 0)
                .orElse(POSITION_MEAL_ALLOWANCE_MAP.getOrDefault(positionName, 0)); // 식대도 고정값 적용

        int bonus = Optional.ofNullable(requestDto.getBonus()).orElse(0);

        log.info("🪾 Attendance 조회 요청 - 대상 userId={}, 로그인한 userId={}, HR 여부={}",
                userId, userInfo.getEmployeeNo(), userInfo.isHrAdmin());
        log.info("🚀 급여 저장 요청: userId={}, year={}, month={}", userId, payYear, payMonth);

        CommonResDto<List<AttendanceResDto>> res = attendanceClient.getMonthlyAttendanceForFeign(
                userId, payYear, payMonth,
                userInfo.getEmail(),
                userInfo.getHrRole(),
                userInfo.getEmployeeNo()
        );

        List<AttendanceResDto> attendanceList = res.getResult();
        long totalWorkMinutes = attendanceList.stream()
                .mapToLong(dto -> parseToMinutes(dto.getTotalWorkTime()))
                .sum();

        log.info("🔍 결정된 positionName: {}", positionName);

        Integer defaultBasePayroll = POSITION_BASE_PAY_MAP.get(positionName);
        int basePayroll = Optional.ofNullable(requestDto.getBasePayroll()).filter(b -> b != 0)
                .orElse(Optional.ofNullable(defaultBasePayroll).orElse(0));

        double hourlyWage = basePayroll / 209.0;
        long totalOvertimeMinutes = attendanceList.stream()
                .mapToLong(dto -> parseToMinutes(dto.getOvertimeWorkTime()) + parseToMinutes(dto.getNightWorkTime()))
                .sum();

        long overtimePay = (totalOvertimeMinutes >= 60)
                ? Math.round((totalOvertimeMinutes / 60.0) * hourlyWage * 1.5) : 0;

        Payroll payroll = existing.orElseGet(Payroll::new);

        payroll.setUserId(userId);
        payroll.setPayYear(payYear);
        payroll.setPayMonth(payMonth);
        payroll.setBasePayroll(basePayroll);

        if (existing.isPresent()) {
            Payroll old = existing.get();
            payroll.setPositionAllowance(requestDto.getPositionAllowance() != null ? requestDto.getPositionAllowance() : old.getPositionAllowance());
            payroll.setMealAllowance(requestDto.getMealAllowance() != null ? requestDto.getMealAllowance() : old.getMealAllowance());
            payroll.setBonus(requestDto.getBonus() != null ? requestDto.getBonus() : old.getBonus());
        } else {
            payroll.setPositionAllowance(positionAllowance);
            payroll.setMealAllowance(mealAllowance);
            payroll.setBonus(bonus);
        }

        long finalPay = basePayroll + positionAllowance + mealAllowance + bonus + overtimePay;

        payroll.setFinalPayAmount(finalPay);
        payroll.setOvertimePay((int) overtimePay);
        payroll.setTotalWorkMinutes(totalWorkMinutes);

        return toDto(payrollRepository.save(payroll));
    }

    @Override
    public PayrollResponseDto getPayrollByUserId(Long userId) {
        Payroll payroll = payrollRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 직원의 급여 정보가 없습니다."));

        if (payroll.getBasePayroll() == null || payroll.getBasePayroll() == 0) {
            String positionName = getUserPosition(userId);
            Integer basePay = POSITION_BASE_PAY_MAP.getOrDefault(positionName, 0);
            payroll.setBasePayroll(basePay);
            payrollRepository.save(payroll);
            log.info("💾 기본급 자동 설정 및 DB 저장 완료: userId={}, position={}, basePay={}", userId, positionName, basePay);
        }

        return toDto(payroll);
    }

    @Override
    public PayrollResponseDto updatePayroll(PayrollRequestDto requestDto) {
        Payroll payroll = payrollRepository.findByUserIdAndPayYearAndPayMonth(
                        requestDto.getUserId(), requestDto.getPayYear(), requestDto.getPayMonth())
                .orElseThrow(() -> new IllegalArgumentException("수정할 급여 정보가 없습니다."));

        payroll.setBasePayroll(Optional.ofNullable(requestDto.getBasePayroll()).orElse(payroll.getBasePayroll()));
        payroll.setPositionAllowance(Optional.ofNullable(requestDto.getPositionAllowance()).orElse(payroll.getPositionAllowance()));
        payroll.setMealAllowance(Optional.ofNullable(requestDto.getMealAllowance()).orElse(payroll.getMealAllowance()));
        payroll.setBonus(Optional.ofNullable(requestDto.getBonus()).orElse(payroll.getBonus()));

        return toDto(payrollRepository.save(payroll));
    }

    @Override
    public void deletePayroll(Long userId, int payYear, int payMonth) {
        Payroll payroll = payrollRepository.findByUserIdAndPayYearAndPayMonth(userId, payYear, payMonth)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 급여 정보가 없습니다."));
        payrollRepository.delete(payroll);
    }

    @Override
    public PayrollResponseDto getPayrollByMonth(Long userId, int year, int month) {
        Payroll payroll = payrollRepository.findByUserIdAndPayYearAndPayMonth(userId, year, month)
                .orElseThrow(() -> new IllegalArgumentException("해당 월의 급여 정보가 존재하지 않습니다."));

        if (payroll.getBasePayroll() == null || payroll.getBasePayroll() == 0) {
            String positionName = getUserPosition(userId);
            Integer basePay = POSITION_BASE_PAY_MAP.getOrDefault(positionName, 0);
            payroll.setBasePayroll(basePay);
            payrollRepository.save(payroll);
            log.info("💾 기본급 자동 설정 및 DB 저장 완료: userId={}, yearMonth={}/{} position={}, basePay={}",
                    userId, year, month, positionName, basePay);
        }

        return toDto(payroll);
    }

    private long parseToMinutes(String timeStr) {
        if (timeStr == null || !timeStr.contains(":")) return 0;
        try {
            String[] parts = timeStr.split(":");
            return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
        } catch (Exception e) {
            log.warn("근무 시간 파싱 실패: {}", timeStr);
            return 0;
        }
    }

    private List<UserResDto> getAllActiveUsersFromHR() {
        List<UserResDto> allUsers = new ArrayList<>();
        int page = 0, size = 100;

        while (true) {
            CommonResDto<HrClient.PageWrapper<UserResDto>> res = hrClient.getUserList(page, size);
            HrClient.PageWrapper<UserResDto> pageData = res.getResult();

            if (pageData == null || pageData.getContent().isEmpty()) break;

            List<UserResDto> active = pageData.getContent().stream()
                    .filter(u -> "Y".equals(u.getActivate()))
                    .toList();

            allUsers.addAll(active);

            if (page >= pageData.getTotalPages() - 1) break;
            page++;
        }

        return allUsers;
    }

    public void generateMonthlyPayrollForAll() {
        int currentYear = LocalDate.now().getYear();
        int currentMonth = LocalDate.now().getMonthValue();

        log.info("📌 스케줄러: {}년 {}월 전체 재직자 급여 자동 생성 시작 (System 계정 사용)", currentYear, currentMonth);

        List<UserResDto> users = getAllActiveUsersFromHR();

        for (UserResDto user : users) {
            try {
                log.info("💼 {} ({}) 급여 생성 시도 중...", user.getUserName(), user.getPositionName());
                PayrollRequestDto dto = PayrollRequestDto.builder()
                        .userId(user.getEmployeeNo())
                        .payYear(currentYear)
                        .payMonth(currentMonth)
                        .positionName(user.getPositionName())
                        .build();

                // 시스템 권한으로 처리
                savePayroll(dto, TokenUserInfo.system());

            } catch (Exception e) {
                log.warn("급여 생성 실패 - userId={}: {}", user.getEmployeeNo(), e.getMessage());
            }
        }

        log.info("✅ {}년 {}월 전체 재직자 급여 자동 생성 완료", currentYear, currentMonth);
    }



    private String getUserPosition(Long userId) {
        try {
            CommonResDto<UserResDto> res = hrClient.getUserById(userId);
            if (res.getResult() != null) {
                return res.getResult().getPositionName();
            }
        } catch (Exception e) {
            log.warn("직급 조회 실패: userId={}, error={}", userId, e.getMessage());
        }
        return null;
    }

    private PayrollResponseDto toDto(Payroll payroll) {
        return PayrollResponseDto.builder()
                .payrollId(payroll.getPayrollId())
                .userId(payroll.getUserId())
                .basePayroll(payroll.getBasePayroll())
                .positionAllowance(payroll.getPositionAllowance())
                .mealAllowance(payroll.getMealAllowance())
                .bonus(payroll.getBonus())
                .overtimePay(payroll.getOvertimePay())
                .payYear(payroll.getPayYear())
                .payMonth(payroll.getPayMonth())
                .totalWorkMinutes(payroll.getTotalWorkMinutes())
                .finalPayAmount(payroll.getFinalPayAmount())
                .updatedAt(payroll.getUpdatedAt())
                .build();
    }
}
