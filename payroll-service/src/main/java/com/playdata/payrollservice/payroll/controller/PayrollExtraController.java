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
    @GetMapping("/{extraId}")
    public ResponseEntity<PayrollExtra> getExtraById(@PathVariable Long extraId) {
        return ResponseEntity.ok(payrollExtraService.getExtraById(extraId));
    }


    // 사용자 정보 포함된 수당 조회
    @GetMapping("/{extraId}/detail")
    public ResponseEntity<PayrollExtraDetailDto> getExtraDetail(@PathVariable Long extraId) {
        PayrollExtraDetailDto dto = payrollExtraService.getExtraWithUser(extraId);
        return ResponseEntity.ok(dto);
    }

    // 수당 수정하기
    @PutMapping("/{extraId}")
    public ResponseEntity<PayrollExtra> updateExtra(@PathVariable Long extraId, @RequestBody PayrollExtraRequestDto dto) {
        PayrollExtra updated = payrollExtraService.updateExtra(extraId, dto);
        return ResponseEntity.ok(updated);
    }

    // 수당 삭제하기
    @DeleteMapping("/{extraId}")
    public ResponseEntity<Void> deleteExtra(@PathVariable Long extraId) {
        payrollExtraService.deleteExtra(extraId);
        return ResponseEntity.noContent().build();
    }


}
