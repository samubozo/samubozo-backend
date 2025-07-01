package com.playdata.authservice.auth.dto;

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
    private String password;
    private String roleId;

}
