package com.playdata.vacationservice.client.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class WorkStatusCreateRequestDto {
    private Long userId;
    private LocalDate startDate;
    private LocalDate endDate;
    private String vacationType; // 예: ANNUAL_LEAVE, AM_HALF_DAY
    private String reason;
}
