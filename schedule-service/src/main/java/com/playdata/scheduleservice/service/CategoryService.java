package com.playdata.scheduleservice.service;

import com.playdata.scheduleservice.dto.CategoryRequest;
import com.playdata.scheduleservice.dto.CategoryResponse;
import com.playdata.scheduleservice.entity.Category;
import com.playdata.scheduleservice.entity.Category.CategoryType;
import com.playdata.scheduleservice.repository.CategoryRepository;
import com.playdata.scheduleservice.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;
import com.playdata.scheduleservice.client.HrServiceClient;
import com.playdata.scheduleservice.dto.UserFeignResDto;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository; // 카테고리 삭제 시 연관된 일정 확인용
    private final HrServiceClient hrServiceClient;

    // 카테고리 생성
    @Transactional
    public CategoryResponse createCategory(Long employeeNo, CategoryRequest request) {
        Long departmentId = getDepartmentId(employeeNo);
        // 권한 검증: 개인 카테고리는 본인만, 그룹 카테고리는 해당 부서원만 생성 가능
        if (request.getType() == CategoryType.PERSONAL && !employeeNo.equals(request.getOwnerEmployeeNo())) {
            throw new IllegalArgumentException("개인 카테고리는 본인만 생성할 수 있습니다.");
        }
        if (request.getType() == CategoryType.GROUP && !departmentId.equals(request.getDepartmentId())) {
            throw new IllegalArgumentException("그룹 카테고리는 소속 부서에서만 생성할 수 있습니다.");
        }

        Category category = Category.builder()
                .name(request.getName())
                .color(request.getColor())
                .type(request.getType())
                .ownerEmployeeNo(request.getType() == CategoryType.PERSONAL ? employeeNo : null)
                .departmentId(request.getType() == CategoryType.GROUP ? departmentId : null)
                .isChecked(true) // 기본적으로 체크된 상태로 생성
                .build();

        return CategoryResponse.from(categoryRepository.save(category));
    }

    // 카테고리 목록 조회
    public List<CategoryResponse> getCategories(Long employeeNo, CategoryType type) {
        Long departmentId = getDepartmentId(employeeNo);
        List<Category> categories;
        if (type == null) {
            // 타입이 지정되지 않으면 개인 카테고리와 그룹 카테고리 모두 조회
            categories = categoryRepository.findByOwnerEmployeeNo(employeeNo);
            if (departmentId != null) {
                categories.addAll(categoryRepository.findByDepartmentId(departmentId));
            }
        } else if (type == CategoryType.PERSONAL) {
            categories = categoryRepository.findByOwnerEmployeeNo(employeeNo);
        } else { // GROUP
            if (departmentId == null) {
                throw new IllegalArgumentException("그룹 카테고리 조회 시 departmentId는 필수입니다.");
            }
            categories = categoryRepository.findByDepartmentId(departmentId);
        }
        return categories.stream()
                .map(CategoryResponse::from)
                .collect(Collectors.toList());
    }

    // 카테고리 수정
    @Transactional
    public CategoryResponse updateCategory(Long categoryId, Long employeeNo, CategoryRequest request) {
        Long departmentId = getDepartmentId(employeeNo);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));

        // 권한 검증
        if (category.getType() == CategoryType.PERSONAL && !category.getOwnerEmployeeNo().equals(employeeNo)) {
            throw new IllegalArgumentException("개인 카테고리를 수정할 권한이 없습니다.");
        }
        if (category.getType() == CategoryType.GROUP && !category.getDepartmentId().equals(departmentId)) {
            throw new IllegalArgumentException("그룹 카테고리를 수정할 권한이 없습니다.");
        }

        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getColor() != null) {
            category.setColor(request.getColor());
        }
        // 타입 변경은 허용하지 않음

        return CategoryResponse.from(categoryRepository.save(category));
    }

    // 카테고리 삭제
    @Transactional
    public void deleteCategory(Long categoryId, Long employeeNo) {
        Long departmentId = getDepartmentId(employeeNo);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));

        // 권한 검증
        if (category.getType() == CategoryType.PERSONAL && !category.getOwnerEmployeeNo().equals(employeeNo)) {
            throw new IllegalArgumentException("개인 카테고리를 삭제할 권한이 없습니다.");
        }
        if (category.getType() == CategoryType.GROUP && !category.getDepartmentId().equals(departmentId)) {
            throw new IllegalArgumentException("그룹 카테고리를 삭제할 권한이 없습니다.");
        }

        // 연관된 일정이 있는지 확인
        if (!eventRepository.findByCategoryId(categoryId).isEmpty()) {
            throw new IllegalStateException("해당 카테고리에 속한 일정이 존재하여 삭제할 수 없습니다.");
        }

        categoryRepository.delete(category);
    }

    // 카테고리 체크박스 상태 업데이트
    @Transactional
    public CategoryResponse updateCategoryCheckbox(Long categoryId, Long employeeNo, Boolean isChecked) {
        Long departmentId = getDepartmentId(employeeNo);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));

        // 권한 검증
        if (category.getType() == CategoryType.PERSONAL && !category.getOwnerEmployeeNo().equals(employeeNo)) {
            throw new IllegalArgumentException("개인 카테고리 체크박스 상태를 변경할 권한이 없습니다.");
        }
        if (category.getType() == CategoryType.GROUP && !category.getDepartmentId().equals(departmentId)) {
            throw new IllegalArgumentException("그룹 카테고리 체크박스 상태를 변경할 권한이 없습니다.");
        }

        category.setIsChecked(isChecked);
        return CategoryResponse.from(categoryRepository.save(category));
    }

    // DTOs (CategoryRequest, CategoryResponse)는 별도 파일로 생성 예정

    // HR Service에서 departmentId를 가져오는 헬퍼 메소드
    private Long getDepartmentId(Long employeeNo) {
        UserFeignResDto user = hrServiceClient.getUserByEmployeeNo(employeeNo);
        if (user == null || user.getDepartmentId() == null) {
            throw new IllegalStateException("사용자의 부서 정보를 찾을 수 없습니다.");
        }
        return user.getDepartmentId();
    }
}