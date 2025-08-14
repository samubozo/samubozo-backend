package com.playdata.hrservice.hr.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentReqDto {
    private String name;
    private String departmentColor;
    private MultipartFile departmentImage;
}
