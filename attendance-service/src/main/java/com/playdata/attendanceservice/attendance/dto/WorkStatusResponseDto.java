package com.playdata.attendanceservice.attendance.dto;

import com.playdata.attendanceservice.attendance.entity.WorkStatus;
import com.playdata.attendanceservice.attendance.entity.WorkStatusType;
import com.playdata.attendanceservice.common.domain.BaseEntity;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class WorkStatusResponseDto {
    private Long id;
    private Long userId;
    private LocalDate date;
    private WorkStatusType statusType;
    private String reason;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    public static WorkStatusResponseDto fromEntity(WorkStatus workStatus) {
        return WorkStatusResponseDto.builder()
                .id(workStatus.getId())
                .userId(workStatus.getUserId())
                .date(workStatus.getDate())
                .statusType(workStatus.getStatusType())
                .reason(workStatus.getReason())
                .createdAt(((BaseEntity) workStatus).getCreatedAt() != null ? ((BaseEntity) workStatus).getCreatedAt().toLocalDate() : null)
                .updatedAt(((BaseEntity) workStatus).getUpdatedAt() != null ? ((BaseEntity) workStatus).getUpdatedAt().toLocalDate() : null)
                .build();
    }
}
