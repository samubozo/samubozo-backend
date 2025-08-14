package com.playdata.authservice.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordDto {

    @NotBlank private String email;
    @NotBlank private String code;
    @NotBlank @Size(min=8, message = "비밀번호는 8자 이상이어야 합니다.")
    private String newPassword;

}
