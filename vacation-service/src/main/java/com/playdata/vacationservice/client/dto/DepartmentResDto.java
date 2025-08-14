package com.playdata.vacationservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder; // 추가
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder // 추가
public class DepartmentResDto {
    private String name;
}
