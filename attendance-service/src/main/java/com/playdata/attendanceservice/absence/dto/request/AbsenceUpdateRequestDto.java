package com.playdata.attendanceservice.absence.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.playdata.attendanceservice.absence.entity.AbsenceType;
import com.playdata.attendanceservice.absence.entity.UrgencyType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class AbsenceUpdateRequestDto {
    @NotNull
    private AbsenceType type;

    private UrgencyType urgency = UrgencyType.NORMAL; // 기본값: 일반

    @NotNull
    @FutureOrPresent
    private LocalDate startDate;

    @NotNull
    @FutureOrPresent
    private LocalDate endDate;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime startTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime endTime;

    private String reason;

    // 전자결재가 필요한 부재인지 확인
    public boolean requiresApproval() {
        return type.requiresApproval();
    }

    // 자동 승인되는 부재인지 확인
    public boolean isSelfApproved() {
        return type.isSelfApproved();
    }

    // 긴급 부재인지 확인
    public boolean isUrgent() {
        return urgency == UrgencyType.URGENT;
    }

    // 전자결재 시스템으로 전송할 데이터 생성 (수정용)
    public ApprovalRequestDto toApprovalRequestDto(Long userId, String userDepartment, Long absenceId) {
        return ApprovalRequestDto.builder()
                .absenceId(absenceId)
                .requestType("ABSENCE")
                .applicantId(userId)
                .applicantDepartment(userDepartment)
                .type(type.name())
                .urgency(urgency.name())
                .startDate(startDate.toString())
                .endDate(endDate.toString())
                .startTime(startTime != null ? startTime.toString() : null)
                .endTime(endTime != null ? endTime.toString() : null)
                .reason(reason)
                .build();
    }
}
