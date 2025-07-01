package com.playdata.hrservice.hr.dto;



import com.fasterxml.jackson.annotation.JsonFormat;
import com.playdata.hrservice.hr.entity.*;
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

    private Long roleId;         // 역할 id (FK)
    private Long departmentId;   // 부서 id (FK)
    private Long positionId;     // 직책 id (FK)

    private String userName;

    @NotEmpty(message = "이메일은 필수입니다!")
    private String email;

    @NotEmpty(message = "비밀번호는 필수입니다!")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    private String password;

    private String address;
    private String phone;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime birthDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate hireDate;

    private String gender;

    private String activate;

    public User toEntity(PasswordEncoder encoder, Department department, Position position, Role role) {
        return User.builder()
                .userName(this.userName)
                .email(this.email)
                .password(encoder.encode(this.password))
                .address(this.address)
                .phone(this.phone)
                .birthDate(this.birthDate)
                .hireDate(this.hireDate)
                .gender(this.gender)
                .activate(this.activate != null ? this.activate : "Y")
                .createdAt(LocalDateTime.now())
                .profileImage(null)
                .department(department)
                .position(position)
                .role(role)
                .build();
    }


}
