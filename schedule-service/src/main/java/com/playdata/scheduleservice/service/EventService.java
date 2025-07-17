package com.playdata.scheduleservice.service;

import com.playdata.scheduleservice.dto.EventRequest;
import com.playdata.scheduleservice.dto.EventResponse;
import com.playdata.scheduleservice.entity.Event;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface EventService {
    // 일정 생성
    @Transactional
    EventResponse createEvent(Long employeeNo, EventRequest request);

    // 월별 일정 조회
    List<EventResponse> getMonthlyEvents(Long employeeNo, int year, int month, Event.EventType type, Long categoryId);

    // 특정 일정 상세 조회
    EventResponse getEventById(Long eventId, Long employeeNo);

    // 일정 수정
    @Transactional
    EventResponse updateEvent(Long eventId, Long employeeNo, EventRequest request);

    // 일정 삭제
    @Transactional
    void deleteEvent(Long eventId, Long employeeNo);

    // 검색 기능
    List<EventResponse> searchEvents(Long employeeNo, String keyword, Long categoryId, LocalDate startDate, LocalDate endDate);

    // HR Service에서 departmentId를 가져오는 헬퍼 메소드
    Long getDepartmentId(Long employeeNo);

    // isAllDay가 true인 모든 일정 조회
    List<EventResponse> getIsAllDayEvents();
}
