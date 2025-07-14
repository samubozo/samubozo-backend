package com.playdata.scheduleservice.dto;

import com.playdata.scheduleservice.entity.Event;
import com.playdata.scheduleservice.entity.Event.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponse {
    private Long id;
    private String title;
    private String content;
    
    private Long categoryId;
    private String categoryName;
    private String categoryColor;
    private EventType type;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isAllDay;
    private Long ownerEmployeeNo;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static EventResponse from(Event event) {
        return EventResponse.builder()
                .id(event.getId())
                .title(event.getTitle())
                .content(event.getContent())
                
                .categoryId(event.getCategory().getId())
                .categoryName(event.getCategory().getName())
                .categoryColor(event.getCategory().getColor())
                .type(event.getType())
                .startDate(event.getStartDate())
                .endDate(event.getEndDate())
                .isAllDay(event.getIsAllDay())
                .ownerEmployeeNo(event.getOwnerEmployeeNo())
                .createdAt(event.getCreatedAt())
                .updatedAt(event.getUpdatedAt())
                .build();
    }
}