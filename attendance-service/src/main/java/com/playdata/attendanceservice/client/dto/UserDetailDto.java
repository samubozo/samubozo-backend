package com.playdata.attendanceservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * HR 서비스로부터 사용자 상세 정보를 받아오기 위한 DTO입니다。
 * 사용자 이름과 부서 정보를 포함합니다。
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailDto {
    private Long userId;
    private String name;
    private String department;
}