package com.playdata.attendanceservice.attendance.dto;

import com.playdata.attendanceservice.attendance.entity.WorkStatusType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class WorkStatusRegisterRequestDto {
    @NotNull
    private Long userId;
    @NotNull
    private LocalDate date;
    @NotNull
    private WorkStatusType statusType;
    private String reason;
}
