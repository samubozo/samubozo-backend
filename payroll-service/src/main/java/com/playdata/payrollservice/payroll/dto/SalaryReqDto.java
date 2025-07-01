package com.playdata.payrollservice.payroll.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaryReqDto {

    private Long userId;
    private Integer baseSalary;
    private Integer positionAllowance;
    private Integer mealAllowance;
}
