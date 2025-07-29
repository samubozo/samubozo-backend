package com.playdata.payrollservice.common.auth;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenUserInfo {

    private String email;
    private Role role;
    private Long employeeNo;

    public boolean isHrAdmin() {
        return role == Role.HR;
    }


    public String getHrRole() {
        return role == Role.HR ? "Y" : "N";
    }
}
