package com.playdata.scheduleservice.service;

import com.playdata.scheduleservice.dto.EventRequest;
import com.playdata.scheduleservice.dto.EventResponse;
import com.playdata.scheduleservice.entity.Category;
import com.playdata.scheduleservice.entity.Event;
import com.playdata.scheduleservice.entity.Event.EventType;
import com.playdata.scheduleservice.repository.CategoryRepository;
import com.playdata.scheduleservice.repository.EventRepository;
import com.playdata.scheduleservice.client.HrServiceClient;
import com.playdata.scheduleservice.dto.UserFeignResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final HrServiceClient hrServiceClient;

    // 일정 생성
    @Transactional
    public EventResponse createEvent(Long employeeNo, EventRequest request) {
        Long departmentId = getDepartmentId(employeeNo);
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("카테고리를 찾을 수 없습니다."));

        // 카테고리 권한 검증
        if (category.getType() == Category.CategoryType.PERSONAL && !category.getOwnerEmployeeNo().equals(employeeNo)) {
            throw new IllegalArgumentException("개인 카테고리에 일정을 생성할 권한이 없습니다.");
        }
        if (category.getType() == Category.CategoryType.GROUP && !category.getDepartmentId().equals(departmentId)) {
            throw new IllegalArgumentException("그룹 카테고리에 일정을 생성할 권한이 없습니다.");
        }

        // 기한 없는 할일 처리 (start, end가 null)
        if (request.getType() == EventType.TODO && request.getStartDate() == null && request.getEndDate() == null) {
            // start_date, end_date가 null인 경우를 허용
        } else if (request.getStartDate() == null) {
            throw new IllegalArgumentException("시작일은 필수입니다.");
        }

        Event event = Event.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .memo(request.getMemo())
                .category(category)
                .type(request.getType())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isAllDay(request.getIsAllDay() != null ? request.getIsAllDay() : false)
                .ownerEmployeeNo(employeeNo)
                .build();

        return EventResponse.from(eventRepository.save(event));
    }

    // 월별 일정 조회
    public List<EventResponse> getMonthlyEvents(Long employeeNo, int year, int month, EventType type, Long categoryId) {
        Long departmentId = getDepartmentId(employeeNo);
        List<Event> events = eventRepository.findMonthlyEvents(employeeNo, departmentId, year, month);

        return events.stream()
                .filter(event -> {
                    boolean matchesType = (type == null || event.getType() == type);
                    boolean matchesCategory = (categoryId == null || event.getCategory().getId().equals(categoryId));
                    return matchesType && matchesCategory;
                })
                .map(EventResponse::from)
                .collect(Collectors.toList());
    }

    // 특정 일정 상세 조회
    public EventResponse getEventById(Long eventId, Long employeeNo) {
        Long departmentId = getDepartmentId(employeeNo);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        // 일정 권한 검증
        if (event.getCategory().getType() == Category.CategoryType.PERSONAL && !event.getOwnerEmployeeNo().equals(employeeNo)) {
            throw new IllegalArgumentException("개인 일정에 접근할 권한이 없습니다.");
        }
        if (event.getCategory().getType() == Category.CategoryType.GROUP && !event.getCategory().getDepartmentId().equals(departmentId)) {
            throw new IllegalArgumentException("그룹 일정에 접근할 권한이 없습니다.");
        }

        return EventResponse.from(event);
    }

    // 일정 수정
    @Transactional
    public EventResponse updateEvent(Long eventId, Long employeeNo, EventRequest request) {
        Long departmentId = getDepartmentId(employeeNo);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        // 일정 권한 검증 (생성자와 동일한 권한 검증)
        if (event.getCategory().getType() == Category.CategoryType.PERSONAL && !event.getOwnerEmployeeNo().equals(employeeNo)) {
            throw new IllegalArgumentException("개인 일정을 수정할 권한이 없습니다.");
        }
        if (event.getCategory().getType() == Category.CategoryType.GROUP && !event.getCategory().getDepartmentId().equals(departmentId)) {
            throw new IllegalArgumentException("그룹 일정을 수정할 권한이 없습니다.");
        }

        // 카테고리 변경 시 권한 검증
        if (request.getCategoryId() != null && !request.getCategoryId().equals(event.getCategory().getId())) {
            Category newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new IllegalArgumentException("새로운 카테고리를 찾을 수 없습니다."));
            if (newCategory.getType() == Category.CategoryType.PERSONAL && !newCategory.getOwnerEmployeeNo().equals(employeeNo)) {
                throw new IllegalArgumentException("새로운 개인 카테고리에 일정을 할당할 권한이 없습니다.");
            }
            if (newCategory.getType() == Category.CategoryType.GROUP && !newCategory.getDepartmentId().equals(departmentId)) {
                throw new IllegalArgumentException("새로운 그룹 카테고리에 일정을 할당할 권한이 없습니다.");
            }
            event.setCategory(newCategory);
        }

        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getContent() != null) event.setContent(request.getContent());
        if (request.getMemo() != null) event.setMemo(request.getMemo());
        if (request.getType() != null) event.setType(request.getType());
        if (request.getStartDate() != null) event.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) event.setEndDate(request.getEndDate());
        if (request.getIsAllDay() != null) event.setIsAllDay(request.getIsAllDay());

        return EventResponse.from(eventRepository.save(event));
    }

    // 일정 삭제
    @Transactional
    public void deleteEvent(Long eventId, Long employeeNo) {
        Long departmentId = getDepartmentId(employeeNo);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다."));

        // 일정 권한 검증
        if (event.getCategory().getType() == Category.CategoryType.PERSONAL && !event.getOwnerEmployeeNo().equals(employeeNo)) {
            throw new IllegalArgumentException("개인 일정을 삭제할 권한이 없습니다.");
        }
        if (event.getCategory().getType() == Category.CategoryType.GROUP && !event.getCategory().getDepartmentId().equals(departmentId)) {
            throw new IllegalArgumentException("그룹 일정을 삭제할 권한이 없습니다.");
        }

        eventRepository.delete(event);
    }

    // 검색 기능
    public List<EventResponse> searchEvents(Long employeeNo, String keyword, Long categoryId, LocalDate startDate, LocalDate endDate) {
        Long departmentId = getDepartmentId(employeeNo);
        List<Event> events = eventRepository.searchEvents(employeeNo, departmentId, keyword, categoryId, startDate, endDate);
        return events.stream()
                .map(EventResponse::from)
                .collect(Collectors.toList());
    }

    // HR Service에서 departmentId를 가져오는 헬퍼 메소드
    private Long getDepartmentId(Long employeeNo) {
        UserFeignResDto user = hrServiceClient.getUserByEmployeeNo(employeeNo);
        if (user == null || user.getDepartmentId() == null) {
            throw new IllegalStateException("사용자의 부서 정보를 찾을 수 없습니다.");
        }
        return user.getDepartmentId();
    }
}