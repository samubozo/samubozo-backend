package com.playdata.payrollservice.payroll.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollRequestDto {

    private Long userId;
    private Integer basePayroll;
    private Integer positionAllowance;
    private Integer mealAllowance;
}
