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

import java.time.LocalDate;

@RestController
@RequestMapping("/payroll")
@RequiredArgsConstructor
@Slf4j
@RefreshScope // spring cloud config가 관리하는 파일의 데이터가 변경되면 빈들을 새로고침해주는 어노테이션
public class PayrollController {

    private final PayrollService payrollService;

    // 1. 급여 정보 등록 (HR만 가능)
    @PostMapping
    public ResponseEntity<?> savePayroll(
            @RequestBody PayrollRequestDto requestDto,
            @RequestAttribute("userInfo") TokenUserInfo userInfo
    ) {
        if (!userInfo.isHrAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("HR만 접근 가능");
        }

        PayrollResponseDto saved = payrollService.savePayroll(requestDto);
        return ResponseEntity.ok(new CommonResDto<>(HttpStatus.OK, "급여 정보 등록 성공!", saved));
    }

    // 2. 특정 직원 급여 정보 조회 (HR만 가능)
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
        return ResponseEntity.ok(
                new CommonResDto<>(HttpStatus.OK, "월별 급여 조회 성공!", payroll));
    }

    // 3. 급여 정보 수정 (HR만 가능)
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

    // 4. 급여 정보 삭제 (HR만 가능)
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

    @GetMapping("/me")
    public ResponseEntity<CommonResDto<PayrollResponseDto>> getMyPayroll(
            @RequestAttribute("userInfo") TokenUserInfo userInfo) {

        log.info("/api/payroll/me: GET");
        log.info("userInfo: {}", userInfo);

        Long userId = userInfo.getEmployeeNo();

        // ✅ 현재 연/월 구해서 전달
        LocalDate now = LocalDate.now();
        int year = now.getYear();
        int month = now.getMonthValue();

        PayrollResponseDto payroll = payrollService.getPayrollByMonth(userId, year, month);

        return ResponseEntity.ok(
                new CommonResDto<>(HttpStatus.OK, "이번 달 급여 조회 성공!", payroll)
        );
    }



    // ✅ 월별 급여 조회 (로그인 사용자 기준)
    @GetMapping("/me/monthly")
    public ResponseEntity<CommonResDto<PayrollResponseDto>> getMyPayrollByMonth(
            @RequestParam int year,
            @RequestParam int month,
            @RequestAttribute("userInfo") TokenUserInfo userInfo
    ) {
        log.info("/api/payroll/me/monthly: GET year={}, month={}", year, month);
        Long userId = userInfo.getEmployeeNo(); // or getUserId()
        PayrollResponseDto payroll = payrollService.getPayrollByMonth(userId, year, month);

        return ResponseEntity.ok(
                new CommonResDto<>(HttpStatus.OK, "월별 급여 조회 성공", payroll));
    }


}








