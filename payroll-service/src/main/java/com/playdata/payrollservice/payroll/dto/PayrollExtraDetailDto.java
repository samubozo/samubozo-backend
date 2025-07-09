package com.playdata.payrollservice.payroll.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PayrollExtraDetailDto {
    private Long extraId;
    private Long userId;
    private String userName;     // userServiceClient.getUserById()로 채움
    private String userEmail;
    private Integer amount;
    private String description;
    private LocalDate dateGiven;

}