package com.playdata.approvalservice.common.auth;

import com.playdata.authservice.common.auth.Role;
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


}
