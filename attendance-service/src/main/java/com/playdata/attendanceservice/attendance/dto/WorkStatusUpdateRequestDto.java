package com.playdata.attendanceservice.attendance.dto;

import com.playdata.attendanceservice.attendance.entity.WorkStatusType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class WorkStatusUpdateRequestDto {
    private LocalDate date;
    private WorkStatusType statusType;
    private String reason;
}
