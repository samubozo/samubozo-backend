package com.playdata.hrservice.hr.dto;



import com.fasterxml.jackson.annotation.JsonFormat;
import com.playdata.hrservice.common.entity.Address;
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

    private String userName;

    @NotEmpty(message = "이메일은 필수입니다!")
    private String email;

    @NotEmpty(message = "비밀번호는 필수입니다!")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    private String password;

    private Address address;
    private String phone;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate hireDate;

    private String gender;

    private Long departmentId;

    private Long positionId;

    /**
     * DTO의 필드만 사용하여 User 엔티티의 '기본 뼈대'를 생성합니다.
     * 연관관계 설정(Department, Position)은 서비스 계층의 책임입니다.
     */
    public User toEntity(PasswordEncoder encoder) {
        return User.builder()
                .userName(this.userName)
                .email(this.email)
                .password(encoder.encode(this.password))
                .address(this.address)
                .phone(this.phone)
                .birthDate(this.birthDate)
                .hireDate(this.hireDate != null ? this.hireDate : LocalDate.now())
                .gender(this.gender)
                .createdAt(LocalDateTime.now())
                .build();
    }


}
