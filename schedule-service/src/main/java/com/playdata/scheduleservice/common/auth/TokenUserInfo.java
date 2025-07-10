package com.playdata.scheduleservice.common.auth;

import lombok.*;

@Setter
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TokenUserInfo {

    private String email;
    private String hrRole;
    private Long employeeNo;


}