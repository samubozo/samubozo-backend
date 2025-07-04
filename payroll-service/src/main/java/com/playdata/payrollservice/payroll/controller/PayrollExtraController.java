package com.playdata.payrollservice.payroll.controller;

import com.playdata.payrollservice.payroll.dto.PayrollExtraDetailDto;
import com.playdata.payrollservice.payroll.dto.PayrollExtraRequestDto;
import com.playdata.payrollservice.payroll.entity.PayrollExtra;
import com.playdata.payrollservice.payroll.service.PayrollExtraService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/payroll/extras")
public class PayrollExtraController {

    private final PayrollExtraService payrollExtraService;

    // 수당 등록하기
    @PostMapping
    public ResponseEntity<PayrollExtra> createExtra(@RequestBody PayrollExtraRequestDto dto) {
       PayrollExtra extra = PayrollExtra.builder()
               .userId(dto.getUserId())
               .amount(dto.getAmount())
               .description(dto.getDescription())
               .dateGiven(dto.getDateGiven())
               .build();

       return ResponseEntity.ok(payrollExtraService.saveExtra(extra));
    }

    // 수당 조회하기
    @GetMapping("/{userId}")
    public ResponseEntity<PayrollExtra> getExtraById(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(payrollExtraService.getExtraById(userId));
    }

    // 사용자 정보 포함된 수당 조회
    @GetMapping("/{userId}/detail")
    public ResponseEntity<PayrollExtraDetailDto> getExtraDetail(@PathVariable("userId") Long userId) {
        PayrollExtraDetailDto dto = payrollExtraService.getExtraWithUser(userId);
        return ResponseEntity.ok(dto);
    }

}
