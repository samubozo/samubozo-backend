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

    public DepartmentResDto(Department department) {
        this.departmentId = department.getDepartmentId();
        this.name = department.getName();
        this.departmentColor = department.getDepartmentColor();
    }
}
