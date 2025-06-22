package com.playdata.payrollservice.payroll.dto;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.playdata.payrollservice.common.auth.Role;
import com.playdata.payrollservice.payroll.entity.User;
import com.playdata.payrollservice.payroll.entity.UserStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSaveReqDto {

    private String role;

    private String name;

    @NotEmpty(message = "이메일은 필수입니다!")
    private String email;

    @NotEmpty(message = "비밀번호는 필수입니다!")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    private String password;

    private String address;
    private String phone;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    public User toEntity(PasswordEncoder encoder) {
        return User.builder()
                .name(this.name)
                .email(this.email)
                .password(encoder.encode(this.password))
                .address(this.address)
                .phone(this.phone)
                .birthDate(this.birthDate)
                .role(Role.valueOf(this.role.toUpperCase()))
                .registeredAt(LocalDateTime.now())
                .status(UserStatus.ACTIVE)
                .build();
    }


}
