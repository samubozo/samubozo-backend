package com.playdata.attendanceservice.attendance.absence.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.playdata.attendanceservice.attendance.absence.entity.Absence;
import com.playdata.attendanceservice.attendance.absence.entity.AbsenceType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class AbsenceResponseDto {

    private Long id;
    private String userId;
    private AbsenceType type;
    private LocalDate startDate;
    private LocalDate endDate;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;
    private String reason;
    private LocalDateTime createdAt;

    public static AbsenceResponseDto from(Absence absence) {
        return AbsenceResponseDto.builder()
                .id(absence.getId())
                .userId(absence.getUserId())
                .type(absence.getType())
                .startDate(absence.getStartDate())
                .endDate(absence.getEndDate())
                .startTime(absence.getStartTime())
                .endTime(absence.getEndTime())
                .reason(absence.getReason())
                .createdAt(absence.getCreatedAt())
                .build();
    }
}
