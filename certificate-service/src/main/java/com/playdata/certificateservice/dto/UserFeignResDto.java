package com.playdata.certificateservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDate;
import java.util.Map;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFeignResDto {

    private Long employeeNo;
    private String userName;
    private String email;
    private String residentRegNo;
    private String externalEmail;
    private String password;
    private String gender;
    private Long departmentId;
    private String departmentName;
    private Long positionId;
    private String positionName;
    private String address;
    private String profileImage;
    private String phone;
    private String remarks;
    private LocalDate birthDate;
    private LocalDate hireDate;
    private LocalDate retireDate;
    private String activate;
    private String hrRole;

    @JsonProperty("department")
    private void unpackDepartment(Map<String, Object> department) {
        if (department != null && department.get("name") != null) {
            this.departmentName = department.get("name").toString();
        }
    }

}
