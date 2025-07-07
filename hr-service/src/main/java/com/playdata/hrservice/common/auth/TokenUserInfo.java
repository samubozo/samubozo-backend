package com.playdata.hrservice.common.auth;

import com.playdata.hrservice.hr.entity.Position;
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
