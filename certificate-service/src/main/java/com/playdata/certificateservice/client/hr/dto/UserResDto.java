package com.playdata.certificateservice.client.hr.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
