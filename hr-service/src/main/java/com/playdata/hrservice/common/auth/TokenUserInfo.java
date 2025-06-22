package com.playdata.hrservice.common.auth;

import com.playdata.hrservice.common.auth.Role;
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
