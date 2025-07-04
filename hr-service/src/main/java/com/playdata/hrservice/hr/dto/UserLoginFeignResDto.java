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
    private String username;
    private String email;
    private String password;
    private String activate;
    private String hrRole;
}
