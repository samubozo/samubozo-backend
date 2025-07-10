package com.playdata.scheduleservice.controller;

import com.playdata.scheduleservice.common.auth.TokenUserInfo;
import com.playdata.scheduleservice.dto.EventRequest;
import com.playdata.scheduleservice.dto.EventResponse;
import com.playdata.scheduleservice.entity.Event.EventType;
import com.playdata.scheduleservice.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    // 일정 생성
    @PostMapping
    public ResponseEntity<EventResponse> createEvent(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo,
            @Valid @RequestBody EventRequest request) {
        EventResponse response = eventService.createEvent(tokenUserInfo.getEmployeeNo(), request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // 월별 일정 조회
    @GetMapping
    public ResponseEntity<List<EventResponse>> getMonthlyEvents(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo,
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) Long categoryId) {
        List<EventResponse> response = eventService.getMonthlyEvents(tokenUserInfo.getEmployeeNo(), year, month, type, categoryId);
        return ResponseEntity.ok(response);
    }

    // 특정 일정 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<EventResponse> getEventById(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo,
            @PathVariable Long id) {
        EventResponse response = eventService.getEventById(id, tokenUserInfo.getEmployeeNo());
        return ResponseEntity.ok(response);
    }

    // 일정 수정
    @PutMapping("/{id}")
    public ResponseEntity<EventResponse> updateEvent(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo,
            @PathVariable Long id,
            @Valid @RequestBody EventRequest request) {
        EventResponse response = eventService.updateEvent(id, tokenUserInfo.getEmployeeNo(), request);
        return ResponseEntity.ok(response);
    }

    // 일정 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEvent(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo,
            @PathVariable Long id) {
        eventService.deleteEvent(id, tokenUserInfo.getEmployeeNo());
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // 검색 기능
    @GetMapping("/search")
    public ResponseEntity<List<EventResponse>> searchEvents(
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<EventResponse> response = eventService.searchEvents(tokenUserInfo.getEmployeeNo(), keyword, categoryId, startDate, endDate);
        return ResponseEntity.ok(response);
    }
}