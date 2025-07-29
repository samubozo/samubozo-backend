package com.playdata.payrollservice.payroll.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollResponseDto {

    private Long payrollId;
    private Long userId;
    private Integer basePayroll;
    private Integer positionAllowance;
    private Integer mealAllowance;
    private Integer bonus;

    private Integer payYear;
    private Integer payMonth;

    private Long totalWorkMinutes;
    private Long finalPayAmount;

    private Integer overtimePay;

    private LocalDateTime updatedAt;

}
