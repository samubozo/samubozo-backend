package com.playdata.attendanceservice.absence.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.playdata.attendanceservice.absence.entity.Absence;
import com.playdata.attendanceservice.absence.entity.AbsenceType;
import com.playdata.attendanceservice.absence.entity.ApprovalStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class AbsenceResponseDto {

    private Long id;
    private Long userId;
    private AbsenceType type;
    private LocalDate startDate;
    private LocalDate endDate;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;
    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;
    private String reason;
    private LocalDateTime createdAt;
    private ApprovalStatus approvalStatus;


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
                .approvalStatus(absence.getApprovalStatus())
                .build();
    }
}
