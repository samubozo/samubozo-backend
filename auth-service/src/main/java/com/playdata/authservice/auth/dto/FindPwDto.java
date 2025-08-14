package com.playdata.authservice.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class FindPwDto {

    @NotBlank(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

}
