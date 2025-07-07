package com.playdata.approvalservice.common.auth;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenUserInfo {

    private String email;
    private Role role; // Role 클래스도 같은 패키지에 있어야 합니다.
}