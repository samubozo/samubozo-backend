package com.playdata.messageservice.dto;

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
}