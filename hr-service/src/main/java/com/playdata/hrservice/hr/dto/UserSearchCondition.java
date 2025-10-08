package com.playdata.hrservice.hr.dto;

import lombok.Data;

@Data
public class UserSearchCondition {
    private String userName;
    private String departmentName;
    private String hrRole;
    private String activate;
}
