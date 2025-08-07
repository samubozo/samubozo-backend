package com.playdata.attendanceservice.absence.controller;

import com.playdata.attendanceservice.absence.dto.request.AbsenceRequestDto;
import com.playdata.attendanceservice.absence.dto.request.AbsenceUpdateRequestDto;
import com.playdata.attendanceservice.absence.dto.response.AbsenceResponseDto;
import com.playdata.attendanceservice.absence.entity.ApprovalStatus;
import com.playdata.attendanceservice.absence.service.AbsenceService;
import com.playdata.attendanceservice.common.auth.TokenUserInfo;
import com.playdata.attendanceservice.common.dto.CommonResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest; // PageRequest import 추가
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

    /**
     * API 요청 성공 시 공통 응답 객체를 생성하여 반환하는 헬퍼 메소드입니다.
     */
    private <T> ResponseEntity<CommonResDto<T>> buildSuccessResponse(T data, String message) {
        CommonResDto<T> resDto = new CommonResDto<>(HttpStatus.OK, message, data);
        return ResponseEntity.ok(resDto);
    }

    /**
     * API 요청 성공 시 공통 응답 객체를 생성하여 반환하는 헬퍼 메소드입니다.
     * HTTP 상태 코드를 직접 지정할 수 있습니다.
     */
    private <T> ResponseEntity<CommonResDto<T>> buildSuccessResponse(HttpStatus status, T data, String message) {
        CommonResDto<T> resDto = new CommonResDto<>(status, message, data);
        return ResponseEntity.status(status).body(resDto);
    }

    // 부재 등록
    @PostMapping
    public ResponseEntity<CommonResDto<AbsenceResponseDto>> createAbsence(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody AbsenceRequestDto request) {
        AbsenceResponseDto createdAbsence = absenceService.createAbsence(userInfo.getEmployeeNo(), request);
        return buildSuccessResponse(HttpStatus.CREATED, createdAbsence, "부재 등록 성공");
    }

    // 내 부재 목록 조회
    @GetMapping("/my")
    public ResponseEntity<CommonResDto<List<AbsenceResponseDto>>> getMyAbsences(
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        List<AbsenceResponseDto> absences = absenceService.getAbsencesByUserId(userInfo.getEmployeeNo());
        return buildSuccessResponse(absences, "내 부재 목록 조회 성공");
    }

    // 단일 부재 상세 조회
    @GetMapping("/{absenceId}")
    public ResponseEntity<CommonResDto<AbsenceResponseDto>> getAbsenceById(
            @PathVariable Long absenceId) {
        AbsenceResponseDto absence = absenceService.getAbsenceById(absenceId);
        return buildSuccessResponse(absence, "부재 상세 조회 성공");
    }

    // 부재 수정
    @PutMapping("/{absenceId}")
    public ResponseEntity<CommonResDto<AbsenceResponseDto>> updateAbsence(
            @PathVariable Long absenceId,
            @RequestBody @Valid AbsenceUpdateRequestDto request,
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        log.info("AbsenceController.updateAbsence called for absenceId: {}, userId: {}", absenceId, userInfo != null ? userInfo.getEmployeeNo() : "null");
        log.info("Request DTO: {}", request);
        AbsenceResponseDto updatedAbsence = absenceService.updateAbsence(absenceId, request, userInfo.getEmployeeNo());
        return buildSuccessResponse(updatedAbsence, "부재 수정 성공");
    }

    // 부재 삭제
    @DeleteMapping("/{absenceId}")
    public ResponseEntity<CommonResDto<Void>> deleteAbsence(
            @PathVariable Long absenceId,
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        absenceService.deleteAbsence(absenceId, userInfo.getEmployeeNo());
        return buildSuccessResponse(HttpStatus.OK, null, "부재 삭제 성공");
    }

    // 부재 승인 (HR용)
    @PostMapping("/{absenceId}/approve")
    public ResponseEntity<CommonResDto<Void>> approveAbsence(
            @PathVariable Long absenceId,
            @RequestParam Long approverId) {
        absenceService.approveAbsence(absenceId, approverId);
        return buildSuccessResponse(HttpStatus.OK, null, "부재 승인 성공");
    }

    // 부재 반려 (HR용)
    @PostMapping("/{absenceId}/reject")
    public ResponseEntity<CommonResDto<Void>> rejectAbsence(
            @PathVariable Long absenceId,
            @RequestParam Long approverId,
            @RequestParam String rejectComment) {
        absenceService.rejectAbsence(absenceId, approverId, rejectComment);
        return buildSuccessResponse(HttpStatus.OK, null, "부재 반려 성공");
    }

    // 결재용 부재 목록 조회 (대기 중)
    @GetMapping("/approval")
    public ResponseEntity<CommonResDto<Page<AbsenceResponseDto>>> getAbsencesForApproval(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AbsenceResponseDto> absences = absenceService.getAbsencesForApproval(pageable);
        return buildSuccessResponse(absences, "결재용 부재 목록 조회 성공");
    }

    // 처리된 부재 목록 조회
    @GetMapping("/processed")
    public ResponseEntity<CommonResDto<Page<AbsenceResponseDto>>> getProcessedAbsences(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<AbsenceResponseDto> absences = absenceService.getProcessedAbsences(pageable);
        return buildSuccessResponse(absences, "처리된 부재 목록 조회 성공");
    }

    // 결재 상태별 부재 목록 조회
    @GetMapping("/status/{approvalStatus}")
    public ResponseEntity<CommonResDto<List<AbsenceResponseDto>>> getAbsencesByStatus(
            @PathVariable ApprovalStatus approvalStatus) {
        List<AbsenceResponseDto> absences = absenceService.getAbsencesByStatus(approvalStatus);
        return buildSuccessResponse(absences, "결재 상태별 부재 목록 조회 성공");
    }

    // 결재자별 처리한 부재 목록 조회
    @GetMapping("/approver/{approverId}")
    public ResponseEntity<CommonResDto<List<AbsenceResponseDto>>> getAbsencesByApprover(
            @PathVariable Long approverId) {
        List<AbsenceResponseDto> absences = absenceService.getAbsencesByApprover(approverId);
        return buildSuccessResponse(absences, "결재자별 처리한 부재 목록 조회 성공");
    }

    // 특정 사용자의 결재 상태별 부재 목록 조회
    @GetMapping("/my/status/{approvalStatus}")
    public ResponseEntity<CommonResDto<List<AbsenceResponseDto>>> getAbsencesByUserIdAndStatus(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @PathVariable ApprovalStatus approvalStatus) {
        List<AbsenceResponseDto> absences = absenceService.getAbsencesByUserIdAndStatus(userInfo.getEmployeeNo(), approvalStatus);
        return buildSuccessResponse(absences, "특정 사용자의 결재 상태별 부재 목록 조회 성공");
    }

    // 날짜 범위별 부재 목록 조회
    @GetMapping("/date-range")
    public ResponseEntity<CommonResDto<List<AbsenceResponseDto>>> getAbsencesByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        List<AbsenceResponseDto> absences = absenceService.getAbsencesByDateRange(startDate, endDate);
        return buildSuccessResponse(absences, "날짜 범위별 부재 목록 조회 성공");
    }

    // 부재 통계 조회
    @GetMapping("/statistics")
    public ResponseEntity<CommonResDto<com.playdata.attendanceservice.absence.dto.response.AbsenceStatisticsDto>> getAbsenceStatistics() {
        com.playdata.attendanceservice.absence.dto.response.AbsenceStatisticsDto statistics = absenceService.getAbsenceStatistics();
        return buildSuccessResponse(statistics, "부재 통계 조회 성공");
    }
}
