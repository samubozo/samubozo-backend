package com.playdata.attendanceservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * HR 서비스로부터 사용자 정보를 받아오기 위한 DTO(Data Transfer Object)입니다.
 * Feign 클라이언트 응답을 이 객체로 매핑합니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {

    // 사용자 ID
    private Long userId;

    // 입사일
    private LocalDate hireDate;
}