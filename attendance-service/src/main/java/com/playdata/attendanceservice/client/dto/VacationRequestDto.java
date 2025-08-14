package com.playdata.attendanceservice.client.dto;

import com.playdata.attendanceservice.attendance.entity.VacationType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * 휴가 신청 요청을 위한 DTO 입니다. (Vacation 서비스로 전송)
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VacationRequestDto {

    private Long userId;
    private VacationType vacationType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}
