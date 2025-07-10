package com.playdata.payrollservice.payroll.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayrollExtraRequestDto {
    private Long userId;
    private Integer amount;
    private String description;
    private LocalDate dateGiven;
}
