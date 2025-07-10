package com.playdata.scheduleservice.dto;

import com.playdata.scheduleservice.entity.Event.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventRequest {

    @NotBlank(message = "일정 제목은 필수입니다.")
    private String title;

    @NotBlank(message = "일정 내용은 필수입니다.")
    private String content;

    private String memo; // Nullable

    @NotNull(message = "카테고리 ID는 필수입니다.")
    private Long categoryId;

    @NotNull(message = "일정 타입은 필수입니다.")
    private EventType type;

    private LocalDate startDate;

    private LocalDate endDate; // Nullable for open-ended todos

    private Boolean isAllDay; // Default to false
}