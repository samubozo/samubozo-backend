package com.playdata.authservice.auth.service;

import com.playdata.authservice.auth.dto.UserLoginFeignResDto;
import com.playdata.authservice.auth.dto.UserLoginReqDto;
import jakarta.validation.constraints.NotBlank;

public interface AuthService {

    UserLoginFeignResDto login(UserLoginReqDto dto);

    void sendPasswordResetCode(@NotBlank(message = "이메일을 입력해 주세요.") String email);

    String makeAlphanumericCode(int length);

    void verifyResetCode(@NotBlank String email, @NotBlank String code);

    void resetPassword(@NotBlank String email, @NotBlank String code, @NotBlank String newPassword);

    String mailCheck(String email);

    boolean isBlocked(String email);

    void blockUser(String email);

    int incrementAttemptCount(String email);

}
