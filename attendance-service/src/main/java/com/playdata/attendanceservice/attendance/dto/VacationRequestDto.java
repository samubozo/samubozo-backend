package com.playdata.attendanceservice.attendance.dto;

import com.playdata.attendanceservice.attendance.entity.VacationType;
import lombok.AllArgsConstructor; // 추가
import lombok.Getter;
import lombok.NoArgsConstructor; // 추가

import java.time.LocalDate;

/**
 * 휴가 신청 요청을 위한 DTO 입니다.
 */
@Getter
@AllArgsConstructor // 추가
@NoArgsConstructor // 추가 (기본 생성자도 필요할 수 있으므로 추가)
public class VacationRequestDto {

    private VacationType vacationType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}
