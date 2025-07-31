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

    public static TokenUserInfo system() {
        return TokenUserInfo.builder()
                .employeeNo(-1L) // 시스템 전용 사용자 번호
                .email("system@s.com")
                .role(Role.HR)     // HR 권한 있음
                .build();
    }

}
