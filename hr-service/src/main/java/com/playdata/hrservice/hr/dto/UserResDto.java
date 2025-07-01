package com.playdata.hrservice.hr.dto;



import com.playdata.hrservice.common.auth.Role;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private String gender;
    private Long roleId;
    private String roleName;
    private Long departmentId;
    private String departmentName;
    private Long positionId;
    private String positionName;
    private String address;
    private String profileImage;
    private String phone;
    private LocalDateTime birthDate;
    private LocalDate hireDate;
    private String activate;

}
