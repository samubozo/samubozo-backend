package com.playdata.hrservice.hr.dto;

import com.playdata.hrservice.hr.entity.User;
import lombok.Builder;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Builder
public class UserRequestDto {

    private Long employeeNo;
    private MultipartFile profileImage;

    public User toEntity() {
        return User.builder()
                .employeeNo(employeeNo)
                .build();
    }

}
