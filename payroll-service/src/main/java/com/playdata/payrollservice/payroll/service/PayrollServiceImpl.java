package com.playdata.payrollservice.payroll.service;

import com.playdata.payrollservice.client.AttendanceClient;
import com.playdata.payrollservice.client.HrClient;
import com.playdata.payrollservice.common.auth.TokenUserInfo;
import com.playdata.payrollservice.common.dto.CommonResDto;
import com.playdata.payrollservice.payroll.dto.AttendanceResDto;
import com.playdata.payrollservice.payroll.dto.PayrollRequestDto;
import com.playdata.payrollservice.payroll.dto.PayrollResponseDto;
import com.playdata.payrollservice.payroll.dto.UserResDto;
import com.playdata.payrollservice.payroll.entity.Payroll;
import com.playdata.payrollservice.payroll.repository.PayrollRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollServiceImpl implements PayrollService {

    private final AttendanceClient attendanceClient;
    private final PayrollRepository payrollRepository;

    @Override
    public PayrollResponseDto savePayroll(PayrollRequestDto requestDto, TokenUserInfo userInfo) {
        Long userId = requestDto.getUserId();
        int payYear = requestDto.getPayYear();
        int payMonth = requestDto.getPayMonth();
        int positionAllowance = Optional.ofNullable(requestDto.getPositionAllowance()).orElse(0);
        int mealAllowance = Optional.ofNullable(requestDto.getMealAllowance()).orElse(0);
        int bonus = Optional.ofNullable(requestDto.getBonus()).orElse(0);
        log.info("🧾 Attendance 조회 요청 - 대상 userId={}, 로그인한 userId={}, HR 여부={}",
                userId, userInfo.getEmployeeNo(), userInfo.isHrAdmin());


        log.info("🚀 급여 저장 요청: userId={}, year={}, month={}", userId, payYear, payMonth);


        // 근무시간 조회
        CommonResDto<List<AttendanceResDto>> res =
                attendanceClient.getMonthlyAttendanceForFeign(
                        userId, payYear, payMonth,
                        userInfo.getEmail(),
                        userInfo.getHrRole(), // "HR" 또는 "USER"
                        userInfo.getEmployeeNo()
                );
        List<AttendanceResDto> attendanceList = res.getResult();

        long totalWorkMinutes = attendanceList.stream()
                .mapToLong(dto -> parseToMinutes(dto.getTotalWorkTime()))
                .sum();

        // ✅ 기본급: 직급 우선, HR 입력이 있으면 override
        log.info("🔍 입력된 positionName: {}", requestDto.getPositionName());

        String positionName = requestDto.getPositionName();
        if (positionName == null || !POSITION_BASE_PAY_MAP.containsKey(positionName)) {
            positionName = getUserPosition(userId); // 🔥 HR 연동으로 조회
        }
        log.info("✅ 근태 조회 결과 건수: {}", attendanceList.size());
        if (!attendanceList.isEmpty()) {
            log.info("⏱ 첫번째 근무일 totalWorkTime = {}", attendanceList.get(0).getTotalWorkTime());
        }

        log.info("🔍 결정된 positionName: {}", positionName); // 직급 결정 결과

        Integer defaultBasePayroll = POSITION_BASE_PAY_MAP.get(positionName);
        int basePayroll = (requestDto.getBasePayroll() == null || requestDto.getBasePayroll() == 0)
                ? (defaultBasePayroll != null ? defaultBasePayroll : 0)
                : requestDto.getBasePayroll();

        log.info("💰 기본급 계산 - userId={}, 직급={}, defaultBasePayroll={}, 최종 basePayroll={}",
                userId, positionName, defaultBasePayroll, basePayroll);

        if (basePayroll == 0) {
            log.warn("⚠️ 기본급이 0원으로 계산됨: userId={}, position={}", userId, positionName);
        }

        // ✅ 통상시급 계산 (월 209시간 기준)
        double hourlyWage = basePayroll / 209.0;

        // ✅ 야근시간 (연장 + 심야 근무 합산)
        long totalOvertimeMinutes = attendanceList.stream()
                .mapToLong(dto ->
                        parseToMinutes(dto.getOvertimeWorkTime()) +
                                parseToMinutes(dto.getNightWorkTime())
                )
                .sum();

        long overtimePay = 0;
        if (totalOvertimeMinutes >= 60) {
            overtimePay = Math.round((totalOvertimeMinutes / 60.0) * hourlyWage * 1.5);
        }


        // 저장 or 수정
        Optional<Payroll> existing = payrollRepository.findByUserIdAndPayYearAndPayMonth(userId, payYear, payMonth);
        Payroll payroll = existing.orElseGet(Payroll::new);

        payroll.setUserId(userId);
        payroll.setPayYear(payYear);
        payroll.setPayMonth(payMonth);
        payroll.setBasePayroll(basePayroll);

// ⬇️ 수당 값부터 세팅
        if (existing.isPresent()) {
            Payroll old = existing.get();
            payroll.setPositionAllowance(requestDto.getPositionAllowance() != null
                    ? requestDto.getPositionAllowance() : old.getPositionAllowance());
            payroll.setMealAllowance(requestDto.getMealAllowance() != null
                    ? requestDto.getMealAllowance() : old.getMealAllowance());
            payroll.setBonus(requestDto.getBonus() != null
                    ? requestDto.getBonus() : old.getBonus());
        } else {
            payroll.setPositionAllowance(Optional.ofNullable(requestDto.getPositionAllowance()).orElse(0));
            payroll.setMealAllowance(Optional.ofNullable(requestDto.getMealAllowance()).orElse(0));
            payroll.setBonus(Optional.ofNullable(requestDto.getBonus()).orElse(0));
        }

// ⬇️ 수당 세팅된 후에야 계산 가능
        long finalPay = basePayroll
                + Optional.ofNullable(payroll.getPositionAllowance()).orElse(0)
                + Optional.ofNullable(payroll.getMealAllowance()).orElse(0)
                + Optional.ofNullable(payroll.getBonus()).orElse(0)
                + overtimePay;
        payroll.setFinalPayAmount(finalPay);

        payroll.setOvertimePay((int) overtimePay); // ✅ 누락되었을 가능성 높음

        payroll.setTotalWorkMinutes(totalWorkMinutes);

        log.info("⏰ 총 야근 시간: {}분", totalOvertimeMinutes);


        return toDto(payrollRepository.save(payroll));
    }





    // 2. 급여 정보 조회 (userId 기준)
    @Override
    public PayrollResponseDto getPayrollByUserId(Long userId) {
        Payroll payroll = payrollRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 직원의 급여 정보가 없습니다."));

        if (payroll.getBasePayroll() == null || payroll.getBasePayroll() == 0) {
            String positionName = getUserPosition(userId);
            Integer basePay = POSITION_BASE_PAY_MAP.getOrDefault(positionName, 0);
            payroll.setBasePayroll(basePay);

            // 💾 DB에 저장
            payrollRepository.save(payroll);

            log.info("💾 기본급 자동 설정 및 DB 저장 완료: userId={}, position={}, basePay={}",
                    userId, positionName, basePay);
        }

        return toDto(payroll);
    }

    // 3. 급여 정보 수정
    @Override
    public PayrollResponseDto updatePayroll(PayrollRequestDto requestDto) {
        Payroll payroll = payrollRepository.findByUserIdAndPayYearAndPayMonth(
                        requestDto.getUserId(), requestDto.getPayYear(), requestDto.getPayMonth())
                .orElseThrow(() -> new IllegalArgumentException("수정할 급여 정보가 없습니다."));

        payroll.setBasePayroll(
                requestDto.getBasePayroll() != null ? requestDto.getBasePayroll() : payroll.getBasePayroll()
        );
        payroll.setPositionAllowance(
                requestDto.getPositionAllowance() != null ? requestDto.getPositionAllowance() : payroll.getPositionAllowance()
        );
        payroll.setMealAllowance(
                requestDto.getMealAllowance() != null ? requestDto.getMealAllowance() : payroll.getMealAllowance()
        );
        payroll.setBonus(
                requestDto.getBonus() != null ? requestDto.getBonus() : payroll.getBonus()
        );


        return toDto(payrollRepository.save(payroll));
    }

    // 4. 급여 정보 삭제
    @Override
    public void deletePayroll(Long userId, int payYear, int payMonth) {
        Payroll payroll = payrollRepository.findByUserIdAndPayYearAndPayMonth(userId, payYear, payMonth)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 급여 정보가 없습니다."));
        payrollRepository.delete(payroll);
    }

    // 5. 특정 연/월 기준 급여 조회
    @Override
    public PayrollResponseDto getPayrollByMonth(Long userId, int year, int month) {
        Payroll payroll = payrollRepository.findByUserIdAndPayYearAndPayMonth(userId, year, month)
                .orElseThrow(() -> new IllegalArgumentException("해당 월의 급여 정보가 존재하지 않습니다."));

        if (payroll.getBasePayroll() == null || payroll.getBasePayroll() == 0) {
            String positionName = getUserPosition(userId);
            Integer basePay = POSITION_BASE_PAY_MAP.getOrDefault(positionName, 0);
            payroll.setBasePayroll(basePay);

            // 💾 DB에 저장
            payrollRepository.save(payroll);

            log.info("💾 기본급 자동 설정 및 DB 저장 완료: userId={}, yearMonth={}/{} position={}, basePay={}",
                    userId, year, month, positionName, basePay);
        }

        return toDto(payroll);
    }


    // 🔧 totalWorkTime "08:30" 형식 문자열 → 분으로 변환
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

    private static final Map<String, Integer> POSITION_BASE_PAY_MAP = Map.of(
            "사장", 9000000,
            "부장", 7000000,
            "책임", 6000000,
            "선임", 4500000,
            "사원", 3000000
    );

    private final HrClient hrClient;

    private String getUserPosition(Long userId) {
        try {
            CommonResDto<UserResDto> res = hrClient.getUserById(userId);
            log.info("📦 HR 응답: {}", res); // 전체 응답 확인
            if (res.getResult() != null) {
                log.info("✅ HR 직급 정보: userId={}, position={}", userId, res.getResult().getPositionName()); // 직급 확인
                return res.getResult().getPositionName();
            } else {
                log.warn("⚠️ HR 응답에서 result가 null: userId={}", userId);
            }
        } catch (Exception e) {
            log.warn("❌ 직급 조회 실패: userId={}, error={}", userId, e.getMessage());
        }
        return null;
    }




    // 🧾 Entity → Dto 변환
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
