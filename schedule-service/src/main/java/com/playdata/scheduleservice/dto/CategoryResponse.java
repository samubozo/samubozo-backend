package com.playdata.scheduleservice.dto;

import com.playdata.scheduleservice.entity.Category;
import com.playdata.scheduleservice.entity.Category.CategoryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryResponse {
    private Long id;
    private String name;
    private String color;
    private CategoryType type;
    private Long ownerEmployeeNo;
    private Long departmentId;
    private Boolean isChecked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CategoryResponse from(Category category) {
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .color(category.getColor())
                .type(category.getType())
                .ownerEmployeeNo(category.getOwnerEmployeeNo())
                .departmentId(category.getDepartmentId())
                .isChecked(category.getIsChecked())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}