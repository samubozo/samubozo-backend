package com.playdata.hrservice.hr.dto;

import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentReqDto {
    private String name;
    private String departmentColor;
}
