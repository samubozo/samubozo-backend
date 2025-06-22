package com.playdata.payrollservice.payroll.dto;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginReqDto {

    private String token;
    private String email;
    private String phone;
    private String address;
    private String role;
    private String password;


}
