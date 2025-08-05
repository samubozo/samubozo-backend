package com.playdata.approvalservice.approval.controller;

import com.playdata.approvalservice.approval.dto.*;
import com.playdata.approvalservice.approval.entity.RequestType;
import com.playdata.approvalservice.approval.service.ApprovalService;
import com.playdata.approvalservice.common.auth.TokenUserInfo;
import com.playdata.approvalservice.common.dto.CommonResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 결재 요청 관련 API를 처리하는 컨트롤러입니다.
 * 타입별로 명확하게 분리된 구조로 리팩터링되었습니다.
 */
@RestController
@RequestMapping("/approvals")
@RequiredArgsConstructor
@Slf4j
public class ApprovalController {

    private final ApprovalService approvalService;

    // ===== 공통 조회 메서드들 =====

    /**
     * 특정 사용자가 특정 날짜에 승인된 휴가의 종류를 조회합니다.
     */
    @GetMapping("/leaves/approved-type")
    public ResponseEntity<String> getApprovedLeaveType(
            @RequestParam("userId") Long userId,
            @RequestParam("date") String date) {
        String leaveType = approvalService.getApprovedLeaveType(userId, LocalDate.parse(date));
        return ResponseEntity.ok(leaveType);
    }

    /**
     * 특정 사용자가 특정 날짜에 승인된 휴가(연차, 반차, 조퇴 등)가 있는지 확인합니다.
     */
    @GetMapping("/leaves/approved")
    public ResponseEntity<Boolean> hasApprovedLeave(
            @RequestParam("userId") Long userId,
            @RequestParam("date") String date) {
        boolean hasLeave = approvalService.hasApprovedLeave(userId, LocalDate.parse(date));
        return ResponseEntity.ok(hasLeave);
    }

    /**
     * 조건에 따라 결재 요청 목록을 동적으로 조회합니다. (페이징 지원)
     * 관리자는 이 API를 사용하여 특정 사용자의 특정 상태(대기/완료)에 있는 특정 종류(휴가/증명서/부재)의 결재 내역을 조회할 수 있습니다.
     * @param applicantId 조회할 직원의 사원 번호 (선택)
     * @param status 조회할 결재 상태 (PENDING, PROCESSED) (선택)
     * @param requestType 조회할 요청 종류 (VACATION, CERTIFICATE, ABSENCE) (선택)
     * @param pageable 페이징 정보 (page, size, sort)
     * @return 조건에 맞는 페이징 처리된 결재 요청 목록
     */
    @GetMapping
    public ResponseEntity<Page<ApprovalRequestResponseDto>> getApprovalRequests(
            @RequestParam(value = "applicantId", required = false) Long applicantId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "requestType", required = false) String requestType,
            Pageable pageable) {
        Page<ApprovalRequestResponseDto> requests = approvalService.getApprovalRequests(applicantId, status, requestType, pageable);
        return ResponseEntity.ok(requests);
    }

    /**
     * 특정 ID를 가진 결재 요청을 조회합니다.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApprovalRequestResponseDto> getApprovalRequestById(@PathVariable Long id) {
        ApprovalRequestResponseDto responseDto = approvalService.getApprovalRequestById(id);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 결재 대기 중인 모든 결재 요청 목록을 조회합니다. (hrRole='Y' 사용자용)
     */
    @GetMapping("/pending")
    public ResponseEntity<Page<ApprovalRequestResponseDto>> getPendingApprovalRequests(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            Pageable pageable) {
        Page<ApprovalRequestResponseDto> pendingRequests = approvalService.getPendingApprovalRequests(userInfo, pageable);
        return ResponseEntity.ok(pendingRequests);
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

    // ===== 휴가 관련 엔드포인트 =====

    /**
     * 새로운 휴가 결재 요청을 생성합니다.
     */
    @PostMapping("/vacation")
    public ResponseEntity<ApprovalRequestResponseDto> createVacationApprovalRequest(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody VacationApprovalRequestCreateDto createDto) {
        ApprovalRequestResponseDto responseDto = approvalService.createVacationApprovalRequest(userInfo, createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    // ===== 증명서 관련 엔드포인트 =====

    /**
     * 새로운 증명서 결재 요청을 생성합니다.
     */
    @PostMapping("/certificate")
    public ResponseEntity<ApprovalRequestResponseDto> createCertificateApprovalRequest(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody CertificateApprovalRequestCreateDto certificateDto) { // 간결해진 DTO를 받습니다.

        // 서비스가 필요로 하는 범용 DTO(ApprovalRequestCreateDto)를 컨트롤러에서 직접 생성합니다.
        ApprovalRequestCreateDto createDto = ApprovalRequestCreateDto.builder()
                .applicantId(userInfo.getEmployeeNo())      // 1. DTO에 없던 applicantId를 토큰에서 설정
                .requestType(RequestType.CERTIFICATE)       // 2. DTO에 없던 requestType을 서버에서 직접 설정
                .title(certificateDto.getTitle())           // 3. 나머지 정보는 받은 DTO에서 가져옴
                .reason(certificateDto.getReason())
                .certificateId(certificateDto.getCertificateId())
                .build();

        // 이제 특정 타입이 아닌, 범용 결재 생성 서비스를 호출합니다.
        ApprovalRequestResponseDto responseDto = approvalService.createApprovalRequest(userInfo, createDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }

    // ===== 부재 관련 엔드포인트 =====

    /**
     * 새로운 부재 결재 요청을 생성합니다.
     */
    @PostMapping("/absence")
    public ResponseEntity<CommonResDto<ApprovalRequestResponseDto>> createAbsenceApprovalRequest(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody AbsenceApprovalRequestCreateDto createDto) {

        ApprovalRequestResponseDto responseDto = approvalService.createAbsenceApprovalRequest(userInfo, createDto);

        // [수정된 부분]
        // 숫자 1 대신, 생성(POST)에 성공했다는 의미인 HttpStatus.CREATED를 사용합니다.
        // 상태 메시지도 조금 더 명확하게 바꿨습니다.
        CommonResDto<ApprovalRequestResponseDto> finalResponse =
                new CommonResDto<>(HttpStatus.CREATED, "결재 요청이 성공적으로 생성되었습니다.", responseDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(finalResponse);
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

    // ===== 공통 승인/반려 엔드포인트 =====

    /**
     * 특정 결재 요청을 승인 처리합니다.
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApprovalRequestResponseDto> approveApprovalRequest(
            @PathVariable Long id,
            @RequestHeader("X-User-Employee-No") Long employeeNo) {
        ApprovalRequestResponseDto responseDto = approvalService.approveApprovalRequest(id, employeeNo);
        return ResponseEntity.ok(responseDto);
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
        return ResponseEntity.ok(responseDto);
    }

    // ===== 타입별 특화 엔드포인트 =====

    /**
     * 특정 타입의 결재 요청 목록을 조회합니다.
     */
    @GetMapping("/requests/{requestType}")
    public ResponseEntity<List<ApprovalRequestResponseDto>> getAllApprovalRequestsByType(
            @PathVariable RequestType requestType) {
        List<ApprovalRequestResponseDto> requests = approvalService.getAllApprovalRequests(requestType);
        return ResponseEntity.ok(requests);
    }

    /**
     * 특정 부재 결재 요청을 승인 처리합니다.
     */
    @PutMapping("/absence/{id}/approve")
    public ResponseEntity<ApprovalRequestResponseDto> approveAbsenceRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        log.info("Approving absence request id: {} by user: {}", id, userInfo.getEmployeeNo());
        ApprovalRequestResponseDto responseDto = approvalService.approveAbsenceApprovalRequest(id, userInfo.getEmployeeNo());
        return ResponseEntity.ok(responseDto);
    }

    /**
     * 특정 부재 결재 요청을 반려 처리합니다.
     */
    @PutMapping("/absence/{id}/reject")
    public ResponseEntity<ApprovalRequestResponseDto> rejectAbsenceRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody ApprovalRejectRequestDto rejectRequestDto) {
        log.info("Rejecting absence request id: {} by user: {}", id, userInfo.getEmployeeNo());
        ApprovalRequestResponseDto responseDto = approvalService.rejectAbsenceApprovalRequest(id, userInfo, rejectRequestDto);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * [신규 추가] 특정 부재 결재 요청을 수정합니다.
     */
    @PutMapping("/absence/{id}")
    public ResponseEntity<ApprovalRequestResponseDto> updateAbsenceApprovalRequest(
            @PathVariable Long id,
            @RequestBody com.playdata.approvalservice.client.dto.AbsenceApprovalRequestUpdateDto updateDto,
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        log.info("Updating absence approval request id: {} by user: {}", id, userInfo.getEmployeeNo());
        ApprovalRequestResponseDto responseDto = approvalService.updateAbsenceApprovalRequest(id, updateDto, userInfo);
        return ResponseEntity.ok(responseDto);
    }

    /**
     * [신규 추가] 특정 결재 요청을 취소(삭제)합니다.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelApprovalRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        log.info("Cancelling approval request id: {} by user: {}", id, userInfo.getEmployeeNo());
        approvalService.cancelApprovalRequest(id, userInfo);
        return ResponseEntity.noContent().build(); // 204 No Content 응답
    }
}