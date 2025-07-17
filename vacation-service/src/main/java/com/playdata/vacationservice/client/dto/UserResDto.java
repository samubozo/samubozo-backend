package com.playdata.vacationservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResDto {

    private Long employeeNo;
    private String userName;
    private String email;
    private String externalEmail;
    private String password;
    private String gender;
    private String residentRegNo;
    private DepartmentResDto department;
    private Long positionId;
    private String positionName;
    private String address;
    private String remarks;
    private String profileImage;
    private String phone;
    private LocalDate birthDate;
    private LocalDate hireDate;
    private LocalDate retireDate;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private String activate;
    private String hrRole;
}