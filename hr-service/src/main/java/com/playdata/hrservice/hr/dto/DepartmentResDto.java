package com.playdata.hrservice.hr.dto;

import com.playdata.hrservice.hr.entity.Department;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentResDto {
    private Long departmentId;
    private String name;
    private String departmentColor;
    private String imageUrl;

    // QueryDSL의 UserListResDto에서 사용할, id와 name만 받는 생성자
    public DepartmentResDto(Long departmentId, String name) {
        this.departmentId = departmentId;
        this.name = name;
    }


    public DepartmentResDto(Department department) {
        this.departmentId = department.getDepartmentId();
        this.name = department.getName();
        this.departmentColor = department.getDepartmentColor();
        this.imageUrl = department.getImageUrl();
    }
}
