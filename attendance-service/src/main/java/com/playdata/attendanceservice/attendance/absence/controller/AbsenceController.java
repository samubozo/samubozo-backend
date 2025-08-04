package com.playdata.attendanceservice.attendance.absence.controller;

import com.playdata.attendanceservice.attendance.absence.dto.request.AbsenceRequestDto;
import com.playdata.attendanceservice.attendance.absence.dto.request.AbsenceUpdateRequestDto;
import com.playdata.attendanceservice.attendance.absence.dto.response.AbsenceResponseDto;
import com.playdata.attendanceservice.attendance.absence.entity.ApprovalStatus;
import com.playdata.attendanceservice.attendance.absence.service.AbsenceService;
import com.playdata.attendanceservice.common.auth.TokenUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/attendance/absence")
@RequiredArgsConstructor
@Slf4j
public class AbsenceController {

    private final AbsenceService absenceService;

    // 부재 등록
    @PostMapping
    public ResponseEntity<AbsenceResponseDto> createAbsence(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody AbsenceRequestDto request) {
        // userInfo가 null인 경우, GlobalExceptionHandler에서 처리되도록 예외를 던집니다.
        // 또는 서비스 계층에서 비즈니스 예외로 처리할 수 있습니다.
        // 여기서는 간단히 NullPointerException이 발생하도록 둡니다.
        return ResponseEntity.status(HttpStatus.CREATED).body(absenceService.createAbsence(userInfo.getEmployeeNo(), request));
    }

    // 내 부재 목록 조회
    @GetMapping("/my")
    public ResponseEntity<List<AbsenceResponseDto>> getMyAbsences(
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        return ResponseEntity.ok(absenceService.getAbsencesByUserId(userInfo.getEmployeeNo()));
    }

    // 단일 부재 상세 조회
    @GetMapping("/{absenceId}")
    public ResponseEntity<AbsenceResponseDto> getAbsenceById(
            @PathVariable Long absenceId) {
        return ResponseEntity.ok(absenceService.getAbsenceById(absenceId));
    }

    // 부재 수정
    @PutMapping("/{absenceId}")
    public ResponseEntity<AbsenceResponseDto> updateAbsence(
            @PathVariable Long absenceId,
            @RequestBody @Valid AbsenceUpdateRequestDto request,
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        log.info("AbsenceController.updateAbsence called for absenceId: {}, userId: {}", absenceId, userInfo != null ? userInfo.getEmployeeNo() : "null");
        log.info("Request DTO: {}", request);
        return ResponseEntity.ok(absenceService.updateAbsence(absenceId, request, userInfo.getEmployeeNo()));
    }

    // 부재 삭제
    @DeleteMapping("/{absenceId}")
    public ResponseEntity<Void> deleteAbsence(
            @PathVariable Long absenceId,
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        absenceService.deleteAbsence(absenceId, userInfo.getEmployeeNo());
        return ResponseEntity.noContent().build();
    }

    // 부재 승인 (HR용)
    @PostMapping("/{absenceId}/approve")
    public ResponseEntity<Void> approveAbsence(
            @PathVariable Long absenceId,
            @RequestParam Long approverId) {
        absenceService.approveAbsence(absenceId, approverId);
        return ResponseEntity.ok().build();
    }

    // 부재 반려 (HR용)
    @PostMapping("/{absenceId}/reject")
    public ResponseEntity<Void> rejectAbsence(
            @PathVariable Long absenceId,
            @RequestParam Long approverId,
            @RequestParam String rejectComment) {
        absenceService.rejectAbsence(absenceId, approverId, rejectComment);
        return ResponseEntity.ok().build();
    }

    // 결재용 부재 목록 조회 (대기 중)
    @GetMapping("/approval")
    public ResponseEntity<Page<AbsenceResponseDto>> getAbsencesForApproval(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ResponseEntity.ok(absenceService.getAbsencesForApproval(pageable));
    }

    // 처리된 부재 목록 조회
    @GetMapping("/processed")
    public ResponseEntity<Page<AbsenceResponseDto>> getProcessedAbsences(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        return ResponseEntity.ok(absenceService.getProcessedAbsences(pageable));
    }

    // 결재 상태별 부재 목록 조회
    @GetMapping("/status/{approvalStatus}")
    public ResponseEntity<List<AbsenceResponseDto>> getAbsencesByStatus(
            @PathVariable ApprovalStatus approvalStatus) {
        return ResponseEntity.ok(absenceService.getAbsencesByStatus(approvalStatus));
    }

    // 결재자별 처리한 부재 목록 조회
    @GetMapping("/approver/{approverId}")
    public ResponseEntity<List<AbsenceResponseDto>> getAbsencesByApprover(
            @PathVariable Long approverId) {
        return ResponseEntity.ok(absenceService.getAbsencesByApprover(approverId));
    }

    // 특정 사용자의 결재 상태별 부재 목록 조회
    @GetMapping("/my/status/{approvalStatus}")
    public ResponseEntity<List<AbsenceResponseDto>> getAbsencesByUserIdAndStatus(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable ApprovalStatus approvalStatus) {
        return ResponseEntity.ok(absenceService.getAbsencesByUserIdAndStatus(userInfo.getEmployeeNo(), approvalStatus));
    }

    // 날짜 범위별 부재 목록 조회
    @GetMapping("/date-range")
    public ResponseEntity<List<AbsenceResponseDto>> getAbsencesByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        return ResponseEntity.ok(absenceService.getAbsencesByDateRange(startDate, endDate));
    }

    // 부재 통계 조회
    @GetMapping("/statistics")
    public ResponseEntity<com.playdata.attendanceservice.attendance.absence.dto.response.AbsenceStatisticsDto> getAbsenceStatistics() {
        return ResponseEntity.ok(absenceService.getAbsenceStatistics());
    }
}
