package com.playdata.scheduleservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFeignResDto {
    private Long employeeNo;
    private String email;
    private String userName;
    private Long departmentId;
    private String departmentName;
    private String position;
}