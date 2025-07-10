package com.playdata.hrservice.hr.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Getter
@Setter
public class UserUpdateRequestDto {

    private String userName;
    private String email;
    private String externalEmail;
    private Long departmentId;
    private String departmentName;
    private Long positionId;
    private String positionName;
    private String address;
    private String remarks;
    private MultipartFile profileImage;
    private String phone;
    private LocalDate birthDate;
    private LocalDate hireDate;
    private LocalDate retireDate;
    private String activate;

}
