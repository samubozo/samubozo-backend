package com.playdata.scheduleservice.dto;

import com.playdata.scheduleservice.entity.Category.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryRequest {

    @NotBlank(message = "카테고리 이름은 필수입니다.")
    private String name;

    @NotBlank(message = "카테고리 색상은 필수입니다.")
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$", message = "색상 코드는 #RRGGBB 또는 #RGB 형식이어야 합니다.")
    private String color;

    @NotNull(message = "카테고리 타입은 필수입니다.")
    private CategoryType type;

    // 개인 카테고리 생성 시에는 필요 없지만, 그룹 카테고리 생성 시에는 필요
    private Long ownerEmployeeNo; // 개인 카테고리 생성 시 사용 (인증된 사용자 정보와 일치해야 함)
    private Long departmentId; // 그룹 카테고리 생성 시 사용 (인증된 사용자 부서 정보와 일치해야 함)
}