package com.playdata.hrservice.hr.dto;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginFeignResDto {
    private Long employeeNo;
    private String userName;
    private String email;
    private String password;
    private String activate;
    private String hrRole;
}
