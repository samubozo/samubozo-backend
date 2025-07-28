package com.playdata.payrollservice.payroll.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResDto {

    private Long employeeNo;
    private String userName;
    private String email;
    private String positionName;
    private String profileImage;
    private String phone;
    private LocalDate birthDate;

    private String bankName;
    private String accountNumber;
    private String accountHolder;
}
