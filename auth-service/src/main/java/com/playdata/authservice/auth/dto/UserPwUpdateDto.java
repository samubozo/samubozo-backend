package com.playdata.authservice.auth.dto;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPwUpdateDto {
    private Long employeeNo;
    private String newPw;
}
