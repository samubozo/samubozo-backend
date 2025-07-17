package com.playdata.authservice.auth.service;

import com.playdata.authservice.auth.dto.UserLoginFeignResDto;
import com.playdata.authservice.auth.dto.UserLoginReqDto;
import jakarta.validation.constraints.NotBlank;

public interface AuthService {
    // 로그인 인증
    UserLoginFeignResDto login(UserLoginReqDto dto);

    // 비밀번호 찾기(인증코드 발송)
    void sendPasswordResetCode(@NotBlank(message = "이메일을 입력해 주세요.") String email);

    String makeAlphanumericCode(int length);

    // 비밀번호 재설정 인증코드 검증
    void verifyResetCode(@NotBlank String email, @NotBlank String code);

    // 비밀번호 재설정(최종 적용)
    void resetPassword(@NotBlank String email, @NotBlank String code, @NotBlank String newPassword);

    // 이메일 인증코드 발송
    String mailCheck(String email);

    // 인증 차단 여부 (이메일 인증용)
    boolean isBlocked(String email);

    // 인증 차단 (이메일 인증용)
    void blockUser(String email);

    // 인증 시도 횟수 관리 (이메일 인증용)
    int incrementAttemptCount(String email);
}
