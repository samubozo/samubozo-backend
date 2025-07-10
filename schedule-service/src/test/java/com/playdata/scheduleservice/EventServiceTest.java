package com.playdata.scheduleservice;

import com.playdata.scheduleservice.client.HrServiceClient;
import com.playdata.scheduleservice.dto.EventRequest;
import com.playdata.scheduleservice.dto.EventResponse;
import com.playdata.scheduleservice.dto.UserFeignResDto;
import com.playdata.scheduleservice.entity.Category;
import com.playdata.scheduleservice.entity.Event;
import com.playdata.scheduleservice.entity.Event.EventType;
import com.playdata.scheduleservice.repository.CategoryRepository;
import com.playdata.scheduleservice.repository.EventRepository;
import com.playdata.scheduleservice.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private HrServiceClient hrServiceClient;

    @InjectMocks
    private EventService eventService;

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

        when(hrServiceClient.getUserByEmployeeNo(testEmployeeNo)).thenReturn(testUserFeignResDto);
    }

    @Test
    @DisplayName("일정 생성 성공 - 개인 카테고리")
    void createEventPersonalCategorySuccess() {
        Category personalCategory = Category.builder().id(1L).name("개인").type(Category.CategoryType.PERSONAL).ownerEmployeeNo(testEmployeeNo).build();
        EventRequest request = EventRequest.builder()
                .title("개인 일정")
                .content("내용")
                .categoryId(1L)
                .type(EventType.MEETING)
                .startDate(LocalDate.now())
                .isAllDay(false)
                .build();
        Event savedEvent = Event.builder().id(1L).title("개인 일정").category(personalCategory).ownerEmployeeNo(testEmployeeNo).build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(personalCategory));
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        EventResponse response = eventService.createEvent(testEmployeeNo, request);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("개인 일정", response.getTitle());
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    @DisplayName("일정 생성 성공 - 그룹 카테고리")
    void createEventGroupCategorySuccess() {
        Category groupCategory = Category.builder().id(2L).name("그룹").type(Category.CategoryType.GROUP).departmentId(testDepartmentId).build();
        EventRequest request = EventRequest.builder()
                .title("그룹 일정")
                .content("내용")
                .categoryId(2L)
                .type(EventType.MEETING)
                .startDate(LocalDate.now())
                .isAllDay(false)
                .build();
        Event savedEvent = Event.builder().id(2L).title("그룹 일정").category(groupCategory).ownerEmployeeNo(testEmployeeNo).build();

        when(categoryRepository.findById(2L)).thenReturn(Optional.of(groupCategory));
        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);

        EventResponse response = eventService.createEvent(testEmployeeNo, request);

        assertNotNull(response);
        assertEquals(2L, response.getId());
        assertEquals("그룹 일정", response.getTitle());
        verify(eventRepository, times(1)).save(any(Event.class));
    }

    @Test
    @DisplayName("월별 일정 조회 성공")
    void getMonthlyEventsSuccess() {
        Category personalCategory = Category.builder().id(1L).name("개인").type(Category.CategoryType.PERSONAL).ownerEmployeeNo(testEmployeeNo).build();
        Event event1 = Event.builder().id(1L).title("일정1").category(personalCategory).type(EventType.MEETING).startDate(LocalDate.of(2024, 7, 1)).ownerEmployeeNo(testEmployeeNo).build();

        when(eventRepository.findMonthlyEvents(testEmployeeNo, testDepartmentId, 2024, 7)).thenReturn(Collections.singletonList(event1));

        List<EventResponse> response = eventService.getMonthlyEvents(testEmployeeNo, 2024, 7, null, null);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("일정1", response.get(0).getTitle());
    }

    @Test
    @DisplayName("일정 상세 조회 성공")
    void getEventByIdSuccess() {
        Category personalCategory = Category.builder().id(1L).name("개인").type(Category.CategoryType.PERSONAL).ownerEmployeeNo(testEmployeeNo).build();
        Event event = Event.builder().id(1L).title("상세 일정").category(personalCategory).ownerEmployeeNo(testEmployeeNo).build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        EventResponse response = eventService.getEventById(1L, testEmployeeNo);

        assertNotNull(response);
        assertEquals("상세 일정", response.getTitle());
    }

    @Test
    @DisplayName("일정 수정 성공")
    void updateEventSuccess() {
        Category personalCategory = Category.builder().id(1L).name("개인").type(Category.CategoryType.PERSONAL).ownerEmployeeNo(testEmployeeNo).build();
        Event existingEvent = Event.builder().id(1L).title("기존 일정").category(personalCategory).ownerEmployeeNo(testEmployeeNo).build();
        EventRequest updateRequest = EventRequest.builder().title("수정된 일정").build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(existingEvent);

        EventResponse response = eventService.updateEvent(1L, testEmployeeNo, updateRequest);

        assertNotNull(response);
        assertEquals("수정된 일정", response.getTitle());
        verify(eventRepository, times(1)).save(existingEvent);
    }

    @Test
    @DisplayName("일정 삭제 성공")
    void deleteEventSuccess() {
        Category personalCategory = Category.builder().id(1L).name("개인").type(Category.CategoryType.PERSONAL).ownerEmployeeNo(testEmployeeNo).build();
        Event existingEvent = Event.builder().id(1L).title("삭제할 일정").category(personalCategory).ownerEmployeeNo(testEmployeeNo).build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(existingEvent));

        assertDoesNotThrow(() -> {
            eventService.deleteEvent(1L, testEmployeeNo);
        });
        verify(eventRepository, times(1)).delete(existingEvent);
    }

    @Test
    @DisplayName("일정 검색 성공")
    void searchEventsSuccess() {
        Category personalCategory = Category.builder().id(1L).name("개인").type(Category.CategoryType.PERSONAL).ownerEmployeeNo(testEmployeeNo).build();
        Event event1 = Event.builder().id(1L).title("검색1").content("내용1").category(personalCategory).ownerEmployeeNo(testEmployeeNo).build();

        when(eventRepository.searchEvents(testEmployeeNo, testDepartmentId, "검색", null, null, null)).thenReturn(Collections.singletonList(event1));

        List<EventResponse> response = eventService.searchEvents(testEmployeeNo, "검색", null, null, null);

        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals("검색1", response.get(0).getTitle());
    }
}
