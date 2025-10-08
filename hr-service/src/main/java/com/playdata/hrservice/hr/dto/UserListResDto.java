package com.playdata.hrservice.hr.dto;


import com.querydsl.core.annotations.QueryProjection;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class UserListResDto {
    private Long employeeNo;
    private String userName;
    private String positionName;
    private DepartmentResDto department;
    private LocalDate hireDate;
    private String phone;
    private String email;
    private String address;
    private String activate;


    /**
     * QueryDSL이 이 생성자를 사용하여 조회 결과를 DTO로 직접 변환합니다.
     * [수정] 파라미터 목록이 Repository의 select 절과 정확히 일치해야 합니다.
     */
    @QueryProjection
    public UserListResDto(Long employeeNo, String userName, String positionName, Long departmentId, String departmentName, LocalDate hireDate, String phone, String email, String address, String activate) {
        this.employeeNo = employeeNo;
        this.userName = userName;
        this.positionName = positionName;
        // 받은 id와 name으로 DepartmentResDto를 직접 생성합니다.
        this.department = new DepartmentResDto(departmentId, departmentName);
        this.hireDate = hireDate;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.activate = activate;
    }
}
