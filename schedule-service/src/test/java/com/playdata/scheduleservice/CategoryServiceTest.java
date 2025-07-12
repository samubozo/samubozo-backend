package com.playdata.scheduleservice;

import com.playdata.scheduleservice.client.HrServiceClient;
import com.playdata.scheduleservice.dto.CategoryRequest;
import com.playdata.scheduleservice.dto.CategoryResponse;
import com.playdata.scheduleservice.dto.UserFeignResDto;
import com.playdata.scheduleservice.entity.Category;
import com.playdata.scheduleservice.entity.Category.CategoryType;
import com.playdata.scheduleservice.repository.CategoryRepository;
import com.playdata.scheduleservice.repository.EventRepository;
import com.playdata.scheduleservice.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import com.playdata.scheduleservice.entity.Event;
import com.playdata.scheduleservice.entity.Event;
import com.playdata.scheduleservice.entity.Event;
import com.playdata.scheduleservice.entity.Event;
import com.playdata.scheduleservice.entity.Event;
import com.playdata.scheduleservice.entity.Event;
import com.playdata.scheduleservice.entity.Event;
import com.playdata.scheduleservice.entity.Event;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private EventRepository eventRepository;
    @Mock
    private HrServiceClient hrServiceClient;

    @InjectMocks
    private CategoryService categoryService;

    private Long testEmployeeNo;
    private Long testDepartmentId;
    private UserFeignResDto testUserFeignResDto;

    @BeforeEach
    void setUp() {
        testEmployeeNo = 1L;
        testDepartmentId = 100L;
        testUserFeignResDto = UserFeignResDto.builder()
                .employeeNo(testEmployeeNo)
                .departmentId(testDepartmentId)
                .build();

        // Mock HR Service call
        when(hrServiceClient.getUserByEmployeeNo(testEmployeeNo)).thenReturn(testUserFeignResDto);
    }

    @Test
    @DisplayName("개인 카테고리 생성 성공")
    void createPersonalCategorySuccess() {
        CategoryRequest request = CategoryRequest.builder()
                .name("개인 카테고리")
                .color("#FFFFFF")
                .type(CategoryType.PERSONAL)
                .ownerEmployeeNo(testEmployeeNo) // 본인 소유
                .build();

        Category savedCategory = Category.builder()
                .id(1L)
                .name("개인 카테고리")
                .color("#FFFFFF")
                .type(CategoryType.PERSONAL)
                .ownerEmployeeNo(testEmployeeNo)
                .isChecked(true)
                .build();

        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        CategoryResponse response = categoryService.createCategory(testEmployeeNo, request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("개인 카테고리", response.getName());
        assertEquals(CategoryType.PERSONAL, response.getType());
        assertEquals(testEmployeeNo, response.getOwnerEmployeeNo());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("그룹 카테고리 생성 성공")
    void createGroupCategorySuccess() {
        CategoryRequest request = CategoryRequest.builder()
                .name("그룹 카테고리")
                .color("#000000")
                .type(CategoryType.GROUP)
                .departmentId(testDepartmentId) // 본인 부서 소유
                .build();

        Category savedCategory = Category.builder()
                .id(2L)
                .name("그룹 카테고리")
                .color("#000000")
                .type(CategoryType.GROUP)
                .departmentId(testDepartmentId)
                .isChecked(true)
                .build();

        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        CategoryResponse response = categoryService.createCategory(testEmployeeNo, request);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("그룹 카테고리", response.getName());
        assertEquals(CategoryType.GROUP, response.getType());
        assertEquals(testDepartmentId, response.getDepartmentId());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("개인 카테고리 생성 실패 - 다른 사용자 소유")
    void createPersonalCategoryFailOtherUser() {
        CategoryRequest request = CategoryRequest.builder()
                .name("다른 사용자 카테고리")
                .color("#123456")
                .type(CategoryType.PERSONAL)
                .ownerEmployeeNo(99L) // 다른 사용자 소유
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            categoryService.createCategory(testEmployeeNo, request);
        });
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("그룹 카테고리 생성 실패 - 다른 부서 소유")
    void createGroupCategoryFailOtherDepartment() {
        CategoryRequest request = CategoryRequest.builder()
                .name("다른 부서 카테고리")
                .color("#654321")
                .type(CategoryType.GROUP)
                .departmentId(999L) // 다른 부서 소유
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            categoryService.createCategory(testEmployeeNo, request);
        });
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("모든 카테고리 조회 성공 - 개인 및 그룹")
    void getCategoriesAllSuccess() {
        Category personalCategory = Category.builder().id(1L).name("개인").type(CategoryType.PERSONAL).ownerEmployeeNo(testEmployeeNo).build();
        Category groupCategory = Category.builder().id(2L).name("그룹").type(CategoryType.GROUP).departmentId(testDepartmentId).build();

        when(categoryRepository.findByOwnerEmployeeNo(testEmployeeNo)).thenReturn(Collections.singletonList(personalCategory));
        when(categoryRepository.findByDepartmentId(testDepartmentId)).thenReturn(Collections.singletonList(groupCategory));

        List<CategoryResponse> response = categoryService.getCategories(testEmployeeNo, null);

        assertNotNull(response);
        assertEquals(2, response.size());
        assertTrue(response.stream().anyMatch(c -> c.getType() == CategoryType.PERSONAL));
        assertTrue(response.stream().anyMatch(c -> c.getType() == CategoryType.GROUP));
    }

    @Test
    @DisplayName("개인 카테고리만 조회 성공")
    void getCategoriesPersonalOnlySuccess() {
        Category personalCategory = Category.builder().id(1L).name("개인").type(CategoryType.PERSONAL).ownerEmployeeNo(testEmployeeNo).build();

        when(categoryRepository.findByOwnerEmployeeNo(testEmployeeNo)).thenReturn(Collections.singletonList(personalCategory));

        List<CategoryResponse> response = categoryService.getCategories(testEmployeeNo, CategoryType.PERSONAL);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(CategoryType.PERSONAL, response.get(0).getType());
    }

    @Test
    @DisplayName("그룹 카테고리만 조회 성공")
    void getCategoriesGroupOnlySuccess() {
        Category groupCategory = Category.builder().id(2L).name("그룹").type(CategoryType.GROUP).departmentId(testDepartmentId).build();

        when(categoryRepository.findByDepartmentId(testDepartmentId)).thenReturn(Collections.singletonList(groupCategory));

        List<CategoryResponse> response = categoryService.getCategories(testEmployeeNo, CategoryType.GROUP);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(CategoryType.GROUP, response.get(0).getType());
    }

    @Test
    @DisplayName("카테고리 수정 성공 - 개인 카테고리")
    void updatePersonalCategorySuccess() {
        Category existingCategory = Category.builder().id(1L).name("기존 개인").color("#AAA").type(CategoryType.PERSONAL).ownerEmployeeNo(testEmployeeNo).build();
        CategoryRequest updateRequest = CategoryRequest.builder().name("수정된 개인").color("#BBB").build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(existingCategory);

        CategoryResponse response = categoryService.updateCategory(1L, testEmployeeNo, updateRequest);

        assertNotNull(response);
        assertEquals("수정된 개인", response.getName());
        assertEquals("#BBB", response.getColor());
        verify(categoryRepository, times(1)).save(existingCategory);
    }

    @Test
    @DisplayName("카테고리 수정 실패 - 권한 없음")
    void updateCategoryFailUnauthorized() {
        Category existingCategory = Category.builder().id(1L).name("기존 개인").color("#AAA").type(CategoryType.PERSONAL).ownerEmployeeNo(99L).build();
        CategoryRequest updateRequest = CategoryRequest.builder().name("수정된 개인").color("#BBB").build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));

        assertThrows(IllegalArgumentException.class, () -> {
            categoryService.updateCategory(1L, testEmployeeNo, updateRequest);
        });
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    @DisplayName("카테고리 삭제 성공")
    void deleteCategorySuccess() {
        Category existingCategory = Category.builder().id(1L).name("삭제할 카테고리").type(CategoryType.PERSONAL).ownerEmployeeNo(testEmployeeNo).build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(eventRepository.findByCategoryId(1L)).thenReturn(Collections.emptyList()); // 연관된 일정 없음

        assertDoesNotThrow(() -> {
            categoryService.deleteCategory(1L, testEmployeeNo);
        });
        verify(categoryRepository, times(1)).delete(existingCategory);
    }

    @Test
    @DisplayName("카테고리 삭제 실패 - 연관된 일정 존재")
    void deleteCategoryFailEventsExist() {
        Category existingCategory = Category.builder().id(1L).name("삭제할 카테고리").type(CategoryType.PERSONAL).ownerEmployeeNo(testEmployeeNo).build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(eventRepository.findByCategoryId(1L)).thenReturn(Collections.singletonList(Event.builder().build())); // 연관된 일정 존재

        assertThrows(IllegalStateException.class, () -> {
            categoryService.deleteCategory(1L, testEmployeeNo);
        });
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    @DisplayName("카테고리 체크박스 상태 업데이트 성공")
    void updateCategoryCheckboxSuccess() {
        Category existingCategory = Category.builder().id(1L).name("카테고리").type(CategoryType.PERSONAL).ownerEmployeeNo(testEmployeeNo).isChecked(true).build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existingCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(existingCategory);

        CategoryResponse response = categoryService.updateCategoryCheckbox(1L, testEmployeeNo, false);

        assertNotNull(response);
        assertFalse(response.getIsChecked());
        verify(categoryRepository, times(1)).save(existingCategory);
    }
}
