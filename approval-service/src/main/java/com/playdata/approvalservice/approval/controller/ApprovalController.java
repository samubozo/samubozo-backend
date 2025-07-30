package com.playdata.approvalservice.approval.controller;

import com.playdata.approvalservice.approval.dto.ApprovalRejectRequestDto;
import com.playdata.approvalservice.approval.dto.ApprovalRequestResponseDto;
import com.playdata.approvalservice.approval.dto.CertificateApprovalRequestCreateDto;
import com.playdata.approvalservice.approval.dto.VacationApprovalRequestCreateDto;
import com.playdata.approvalservice.approval.dto.AbsenceApprovalRequestCreateDto;
import com.playdata.approvalservice.approval.dto.AbsenceApprovalStatisticsDto;
import com.playdata.approvalservice.approval.entity.RequestType;
import com.playdata.approvalservice.approval.service.ApprovalService;
import com.playdata.approvalservice.common.auth.TokenUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 결재 요청 관련 API를 처리하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/approvals")
@RequiredArgsConstructor
@Slf4j
public class ApprovalController {

    private final ApprovalService approvalService;

    // 특정 사용자가 특정 날짜에 승인된 휴가의 종류를 조회합니다.
    @GetMapping("/leaves/approved-type")
    public ResponseEntity<String> getApprovedLeaveType(
            @RequestParam("userId") Long userId,
            @RequestParam("date") String date) {
        String leaveType = approvalService.getApprovedLeaveType(userId, LocalDate.parse(date));
        return ResponseEntity.ok(leaveType);
    }

    // 특정 사용자가 특정 날짜에 승인된 휴가(연차, 반차, 조퇴 등)가 있는지 확인합니다.
    @GetMapping("/leaves/approved")
    public ResponseEntity<Boolean> hasApprovedLeave(
            @RequestParam("userId") Long userId,
            @RequestParam("date") String date) {
        boolean hasLeave = approvalService.hasApprovedLeave(userId, LocalDate.parse(date));
        return ResponseEntity.ok(hasLeave);
    }

    /**
     * 새로운 휴가 결재 요청을 생성합니다.
     */
    @PostMapping("/vacation")
    public ResponseEntity<ApprovalRequestResponseDto> createVacationApprovalRequest(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody VacationApprovalRequestCreateDto createDto) {
        ApprovalRequestResponseDto responseDto = approvalService.createVacationApprovalRequest(userInfo, createDto);
        return buildCreatedResponse(responseDto);
    }

    /**
     * 새로운 증명서 결재 요청을 생성합니다.
     */
    @PostMapping("/certificate")
    public ResponseEntity<ApprovalRequestResponseDto> createCertificateApprovalRequest(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody CertificateApprovalRequestCreateDto createDto) {
        ApprovalRequestResponseDto responseDto = approvalService.createCertificateApprovalRequest(userInfo, createDto);
        return buildCreatedResponse(responseDto);
    }

    /**
     * 새로운 부재 결재 요청을 생성합니다.
     */
    @PostMapping("/absence")
    public ResponseEntity<ApprovalRequestResponseDto> createAbsenceApprovalRequest(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody AbsenceApprovalRequestCreateDto createDto) {
        log.info("Creating absence approval request for user: {}", userInfo.getEmployeeNo());
        ApprovalRequestResponseDto responseDto = approvalService.createAbsenceApprovalRequest(userInfo, createDto);
        return buildCreatedResponse(responseDto);
    }

    /**
     * 모든 결재 요청 목록을 조회합니다. 선택적으로 요청 유형(requestType)으로 필터링할 수 있습니다.
     */
    @GetMapping
    public ResponseEntity<List<ApprovalRequestResponseDto>> getAllApprovalRequests(
            @RequestParam(value = "requestType", required = false) String requestTypeStr) {
        List<ApprovalRequestResponseDto> allRequests;
        if (requestTypeStr != null && !requestTypeStr.isEmpty()) {
            try {
                com.playdata.approvalservice.approval.entity.RequestType requestType = com.playdata.approvalservice.approval.entity.RequestType.valueOf(requestTypeStr.toUpperCase());
                allRequests = approvalService.getAllApprovalRequests(requestType);
            } catch (IllegalArgumentException e) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
        } else {
            allRequests = approvalService.getAllApprovalRequests();
        }
        return ResponseEntity.ok(allRequests);
    }

    /**
     * 결재 대기 중인 모든 결재 요청 목록을 조회합니다. (hrRole='Y' 사용자용)
     */
    @GetMapping("/pending")
    public ResponseEntity<List<ApprovalRequestResponseDto>> getPendingApprovalRequests(
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        List<ApprovalRequestResponseDto> pendingRequests = approvalService.getPendingApprovalRequests(userInfo);
        return ResponseEntity.ok(pendingRequests);
    }

    /**
     * 특정 ID를 가진 결재 요청을 조회합니다.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApprovalRequestResponseDto> getApprovalRequestById(@PathVariable Long id) {
        ApprovalRequestResponseDto responseDto = approvalService.getApprovalRequestById(id);
        return buildOkResponse(responseDto);
    }

    /**
     * 특정 결재 요청을 승인 처리합니다.
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApprovalRequestResponseDto> approveApprovalRequest(
            @PathVariable Long id,
            @RequestHeader("X-User-Employee-No") Long employeeNo) {
        ApprovalRequestResponseDto responseDto = approvalService.approveApprovalRequest(id, employeeNo);
        return buildOkResponse(responseDto);
    }

    /**
     * 특정 결재 요청을 반려 처리합니다.
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<ApprovalRequestResponseDto> rejectApprovalRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody ApprovalRejectRequestDto rejectRequestDto) {
        ApprovalRequestResponseDto responseDto = approvalService.rejectApprovalRequest(id, userInfo, rejectRequestDto);
        return buildOkResponse(responseDto);
    }

    // ===== 부재 관련 엔드포인트들 추가 =====

    /**
     * 부재 결재 요청을 승인 처리합니다.
     */
    @PutMapping("/absence/{id}/approve")
    public ResponseEntity<ApprovalRequestResponseDto> approveAbsenceApprovalRequest(
            @PathVariable Long id,
            @RequestHeader("X-User-Employee-No") Long employeeNo) {
        log.info("Approving absence approval request: {}, by employee: {}", id, employeeNo);
        ApprovalRequestResponseDto responseDto = approvalService.approveAbsenceApprovalRequest(id, employeeNo);
        return buildOkResponse(responseDto);
    }

    /**
     * 부재 결재 요청을 반려 처리합니다.
     */
    @PutMapping("/absence/{id}/reject")
    public ResponseEntity<ApprovalRequestResponseDto> rejectAbsenceApprovalRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody ApprovalRejectRequestDto rejectRequestDto) {
        log.info("Rejecting absence approval request: {}, by employee: {}", id, userInfo.getEmployeeNo());
        ApprovalRequestResponseDto responseDto = approvalService.rejectAbsenceApprovalRequest(id, userInfo, rejectRequestDto);
        return buildOkResponse(responseDto);
    }

    @GetMapping("/requests/{requestType}")
    public ResponseEntity<List<ApprovalRequestResponseDto>> getAllApprovalRequestsByType(
            @PathVariable RequestType requestType) {
        List<ApprovalRequestResponseDto> requests = approvalService.getAllApprovalRequests(requestType);
        return ResponseEntity.ok(requests);
    }

    /**
     * 부재 결재 요청 목록을 조회합니다. (페이징 지원)
     */
    @GetMapping("/absence")
    public ResponseEntity<Page<ApprovalRequestResponseDto>> getAbsenceApprovalRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching absence approval requests - page: {}, size: {}", page, size);
        Page<ApprovalRequestResponseDto> requests = approvalService.getAbsenceApprovalRequests(page, size);
        return ResponseEntity.ok(requests);
    }

    /**
     * 대기 중인 부재 결재 요청 목록을 조회합니다. (HR용)
     */
    @GetMapping("/absence/pending")
    public ResponseEntity<Page<ApprovalRequestResponseDto>> getPendingAbsenceApprovalRequests(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching pending absence approval requests for HR - page: {}, size: {}", page, size);
        Page<ApprovalRequestResponseDto> requests = approvalService.getPendingAbsenceApprovalRequests(userInfo, page, size);
        return ResponseEntity.ok(requests);
    }

    /**
     * 처리된 부재 결재 요청 목록을 조회합니다. (페이징 지원)
     */
    @GetMapping("/absence/processed")
    public ResponseEntity<Page<ApprovalRequestResponseDto>> getProcessedAbsenceApprovalRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching processed absence approval requests - page: {}, size: {}", page, size);
        Page<ApprovalRequestResponseDto> requests = approvalService.getProcessedAbsenceApprovalRequests(page, size);
        return ResponseEntity.ok(requests);
    }

    /**
     * 특정 사용자의 부재 결재 요청 목록을 조회합니다.
     */
    @GetMapping("/absence/my")
    public ResponseEntity<Page<ApprovalRequestResponseDto>> getMyAbsenceApprovalRequests(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching my absence approval requests for user: {} - page: {}, size: {}",
                userInfo.getEmployeeNo(), page, size);
        Page<ApprovalRequestResponseDto> requests = approvalService.getMyAbsenceApprovalRequests(userInfo, page, size);
        return ResponseEntity.ok(requests);
    }

    /**
     * 특정 결재자가 처리한 부재 결재 요청 목록을 조회합니다.
     */
    @GetMapping("/absence/processed-by-me")
    public ResponseEntity<Page<ApprovalRequestResponseDto>> getAbsenceApprovalRequestsProcessedByMe(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        log.info("Fetching absence approval requests processed by me - page: {}, size: {}", page, size);
        Page<ApprovalRequestResponseDto> requests = approvalService.getAbsenceApprovalRequestsProcessedByMe(userInfo, page, size);
        return ResponseEntity.ok(requests);
    }

    /**
     * 부재 결재 통계를 조회합니다.
     */
    @GetMapping("/absence/statistics")
    public ResponseEntity<AbsenceApprovalStatisticsDto> getAbsenceApprovalStatistics() {
        log.info("Fetching absence approval statistics");
        AbsenceApprovalStatisticsDto statistics = approvalService.getAbsenceApprovalStatistics();
        return ResponseEntity.ok(statistics);
    }

    /**
     * 특정 결재자가 처리한 모든 결재 요청 목록을 조회합니다. (hrRole='Y' 사용자용)
     */
    @GetMapping("/processed-by-me")
    public ResponseEntity<List<ApprovalRequestResponseDto>> getProcessedApprovalRequestsByApproverId(
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        List<ApprovalRequestResponseDto> processedRequests = approvalService.getProcessedApprovalRequestsByApproverId(userInfo);
        return ResponseEntity.ok(processedRequests);
    }

    /**
     * HTTP 200 OK 응답을 생성하는 헬퍼 메서드입니다.
     */
    private ResponseEntity<ApprovalRequestResponseDto> buildOkResponse(ApprovalRequestResponseDto responseDto) {
        return ResponseEntity.ok(responseDto);
    }

    /**
     * HTTP 201 Created 응답을 생성하는 헬퍼 메서드입니다.
     */
    private ResponseEntity<ApprovalRequestResponseDto> buildCreatedResponse(ApprovalRequestResponseDto responseDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }
}