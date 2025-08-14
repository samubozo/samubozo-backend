package com.playdata.scheduleservice.service;

import com.playdata.scheduleservice.dto.CategoryRequest;
import com.playdata.scheduleservice.dto.CategoryResponse;
import com.playdata.scheduleservice.entity.Category;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface CategoryService {
    // 카테고리 생성
    @Transactional
    CategoryResponse createCategory(Long employeeNo, CategoryRequest request);

    // 카테고리 목록 조회
    List<CategoryResponse> getCategories(Long employeeNo, Category.CategoryType type);

    // 카테고리 수정
    @Transactional
    CategoryResponse updateCategory(Long categoryId, Long employeeNo, CategoryRequest request);

    // 카테고리 삭제
    @Transactional
    boolean deleteCategory(Long categoryId, Long employeeNo);

    // 카테고리 체크박스 상태 업데이트
    @Transactional
    CategoryResponse updateCategoryCheckbox(Long categoryId, Long employeeNo, Boolean isChecked);

    // HR Service에서 departmentId를 가져오는 헬퍼 메소드
    Long getDepartmentId(Long employeeNo);
}
