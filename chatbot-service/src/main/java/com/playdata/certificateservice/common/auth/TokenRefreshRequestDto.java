package com.playdata.certificateservice.common.auth;


import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TokenRefreshRequestDto {

    private String refreshToken;

}
