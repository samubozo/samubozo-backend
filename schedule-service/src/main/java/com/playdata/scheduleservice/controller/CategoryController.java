package com.playdata.scheduleservice.controller;

import com.playdata.scheduleservice.common.auth.TokenUserInfo;
import com.playdata.scheduleservice.dto.CategoryRequest;
import com.playdata.scheduleservice.dto.CategoryResponse;
import com.playdata.scheduleservice.entity.Category.CategoryType;
import com.playdata.scheduleservice.service.CategoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    // 카테고리 생성
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(tokenUserInfo.getEmployeeNo(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 카테고리 목록 조회
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getCategories(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo,
            @RequestParam(required = false) CategoryType type) {
        List<CategoryResponse> response = categoryService.getCategories(tokenUserInfo.getEmployeeNo(), type);
        return ResponseEntity.ok(response);
    }

    // 카테고리 수정
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo,
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.updateCategory(id, tokenUserInfo.getEmployeeNo(), request);
        return ResponseEntity.ok(response);
    }

    // 카테고리 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo,
            @PathVariable Long id) {
        categoryService.deleteCategory(id, tokenUserInfo.getEmployeeNo());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // 카테고리 체크박스 상태 업데이트
    @PatchMapping("/{id}/checkbox")
    public ResponseEntity<CategoryResponse> updateCategoryCheckbox(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo,
            @PathVariable Long id,
            @RequestParam @NotNull Boolean isChecked) {
        CategoryResponse response = categoryService.updateCategoryCheckbox(id, tokenUserInfo.getEmployeeNo(), isChecked);
        return ResponseEntity.ok(response);
    }
}