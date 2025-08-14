package com.playdata.hrservice.hr.dto;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserLoginResDto {
    
    private UserResDto user; // 선택적 필드 (HR Service에서 추가 정보 제공 시)
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long expiresIn; // 토큰 만료 시간 (초)
    
    public static UserLoginResDto of(UserResDto user, String accessToken, String refreshToken, Long expiresIn) {
        return UserLoginResDto.builder()
                .user(user)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .build();
    }
    
    public static UserLoginResDto of(String accessToken, String refreshToken, Long expiresIn) {
        return UserLoginResDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(expiresIn)
                .build();
    }
} 