package com.playdata.vacationservice.vacation.dto;





import com.playdata.vacationservice.common.auth.Role;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResDto {

    private Long userid;
    private String email;
    private String name;
    private Role role;
    private String address;
    private String profileImage;
    private String socialProvider;
    private String phone;
    private LocalDate birthdate;


}
