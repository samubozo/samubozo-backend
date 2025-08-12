package com.playdata.payrollservice.payroll.controller;

import com.playdata.payrollservice.common.auth.TokenUserInfo;
import com.playdata.payrollservice.common.dto.CommonResDto;
import com.playdata.payrollservice.payroll.dto.PayrollRequestDto;
import com.playdata.payrollservice.payroll.dto.PayrollResponseDto;
import com.playdata.payrollservice.payroll.service.PayrollService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payroll")
@RequiredArgsConstructor
@Slf4j
@RefreshScope
public class PayrollController {

    private final PayrollService payrollService;

    @PostMapping
    public ResponseEntity<?> savePayroll(
            @RequestBody PayrollRequestDto requestDto,
            @RequestAttribute("userInfo") TokenUserInfo userInfo
    ) {
        Long requestUserId = requestDto.getUserId();
        Long loginUserId = userInfo.getEmployeeNo();

        log.info("✅ savePayroll() 진입 - 선택한 직원 ID: {}, 로그인 사용자 ID: {}, HR 여부: {}",
                requestUserId, loginUserId, userInfo.isHrAdmin());

        if (!userInfo.isHrAdmin() && !requestUserId.equals(loginUserId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("본인 정보만 수정할 수 있습니다.");
        }

        PayrollResponseDto saved = payrollService.savePayroll(requestDto, userInfo);
        return ResponseEntity.ok(new CommonResDto<>(HttpStatus.OK, "급여 정보 등록 성공!", saved));
    }

    @GetMapping("/admin/monthly")
    public ResponseEntity<?> getPayrollByAdmin(
            @RequestParam Long userId,
            @RequestParam Integer year,
            @RequestParam Integer month,
            @RequestAttribute("userInfo") TokenUserInfo userInfo
    ) {
        if (!userInfo.isHrAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("HR만 접근 가능");
        }

        PayrollResponseDto payroll = payrollService.getPayrollByMonth(userId, year, month);
        return ResponseEntity.ok(new CommonResDto<>(HttpStatus.OK, "월별 급여 조회 성공!", payroll));
    }

    @PutMapping
    public ResponseEntity<?> updatePayroll(
            @RequestBody PayrollRequestDto requestDto,
            @RequestAttribute("userInfo") TokenUserInfo userInfo
    ) {
        if (!userInfo.isHrAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("HR만 접근 가능");
        }

        PayrollResponseDto updated = payrollService.updatePayroll(requestDto);
        return ResponseEntity.ok(new CommonResDto<>(HttpStatus.OK, "급여 정보 수정 성공!", updated));
    }

    @DeleteMapping
    public ResponseEntity<?> deletePayroll(
            @RequestParam Long userId,
            @RequestParam int payYear,
            @RequestParam int payMonth,
            @RequestAttribute("userInfo") TokenUserInfo userInfo
    ) {
        if (!userInfo.isHrAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("HR만 접근 가능");
        }

        payrollService.deletePayroll(userId, payYear, payMonth);
        return ResponseEntity.ok(new CommonResDto<>(HttpStatus.OK, "급여 정보 삭제 성공!", null));
    }


    @GetMapping("/me/monthly")
    public ResponseEntity<CommonResDto<PayrollResponseDto>> getMyPayrollByMonth(
            @RequestParam int year,
            @RequestParam int month,
            @RequestAttribute("userInfo") TokenUserInfo userInfo
    ) {
        log.info("/api/payroll/me/monthly: GET year={}, month={}", year, month);
        PayrollResponseDto payroll = payrollService.getPayrollByMonth(userInfo.getEmployeeNo(), year, month);

        return ResponseEntity.ok(
                new CommonResDto<>(HttpStatus.OK, "월별 급여 조회 성공", payroll));
    }
}
