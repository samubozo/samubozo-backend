package com.playdata.hrservice.hr.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UserUpdateRequestDto {

    private String name;
    private String address;
    private String phone;
    private String email;
    private String password;
    private LocalDate birthDate;

}
