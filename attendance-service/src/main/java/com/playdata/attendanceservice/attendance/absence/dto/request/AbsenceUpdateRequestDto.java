package com.playdata.attendanceservice.attendance.absence.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.playdata.attendanceservice.attendance.absence.entity.AbsenceType;
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
}
