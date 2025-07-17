package com.playdata.approvalservice.approval.controller;

import com.playdata.approvalservice.approval.dto.ApprovalRequestCreateDto;
import com.playdata.approvalservice.approval.dto.ApprovalRequestResponseDto;
import com.playdata.approvalservice.approval.service.ApprovalService;
import com.playdata.approvalservice.common.auth.TokenUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 결재 요청 관련 API를 처리하는 컨트롤러입니다.
 */
@RestController
@RequestMapping("/approvals")
@RequiredArgsConstructor
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
     * 새로운 결재 요청을 생성합니다.
     *
     * @param createDto 결재 요청 생성에 필요한 데이터를 담은 DTO
     * @return 생성된 결재 요청의 응답 DTO와 HTTP 201 Created 상태
     */
    @PostMapping
    public ResponseEntity<ApprovalRequestResponseDto> createApprovalRequest(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody ApprovalRequestCreateDto createDto) {
        // 서비스 메서드 호출 시 userInfo도 함께 전달
        ApprovalRequestResponseDto responseDto = approvalService.createApprovalRequest(userInfo, createDto);
        return buildCreatedResponse(responseDto);
    }

    /**
     * 모든 결재 요청 목록을 조회합니다.
     *
     * @return 모든 결재 요청 목록과 HTTP 200 OK 상태
     */
    @GetMapping
    public ResponseEntity<List<ApprovalRequestResponseDto>> getAllApprovalRequests() {
        List<ApprovalRequestResponseDto> allRequests = approvalService.getAllApprovalRequests();
        return ResponseEntity.ok(allRequests);
    }

    /**
     * 특정 ID를 가진 결재 요청을 조회합니다.
     *
     * @param id 조회할 결재 요청의 ID
     * @return 조회된 결재 요청의 응답 DTO와 HTTP 200 OK 상태
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApprovalRequestResponseDto> getApprovalRequestById(@PathVariable Long id) {
        ApprovalRequestResponseDto responseDto = approvalService.getApprovalRequestById(id);
        return buildOkResponse(responseDto);
    }

    /**
     * 특정 결재 요청을 승인 처리합니다.
     *
     * @param id 승인할 결재 요청의 ID
     * @param approverId 승인자의 ID (실제로는 인증 정보에서 추출)
     * @return 승인 처리된 결재 요청의 응답 DTO와 HTTP 200 OK 상태
     */
    @PutMapping("/{id}/approve")
    public ResponseEntity<ApprovalRequestResponseDto> approveApprovalRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        ApprovalRequestResponseDto responseDto = approvalService.approveApprovalRequest(id, userInfo);
        return buildOkResponse(responseDto);
    }

    /**
     * 특정 결재 요청을 반려 처리합니다.
     *
     * @param id 반려할 결재 요청의 ID
     * @param userInfo 반려자의 정보를 담은 객체
     * @return 반려 처리된 결재 요청의 응답 DTO와 HTTP 200 OK 상태
     */
    @PutMapping("/{id}/reject")
    public ResponseEntity<ApprovalRequestResponseDto> rejectApprovalRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        ApprovalRequestResponseDto responseDto = approvalService.rejectApprovalRequest(id, userInfo);
        return buildOkResponse(responseDto);
    }

    /**
     * HTTP 200 OK 응답을 생성하는 헬퍼 메서드입니다.
     *
     * @param responseDto 응답 본문에 포함될 DTO
     * @return ResponseEntity<ApprovalRequestResponseDto> 객체
     */
    private ResponseEntity<ApprovalRequestResponseDto> buildOkResponse(ApprovalRequestResponseDto responseDto) {
        return ResponseEntity.ok(responseDto);
    }

    /**
     * HTTP 201 Created 응답을 생성하는 헬퍼 메서드입니다.
     *
     * @param responseDto 응답 본문에 포함될 DTO
     * @return ResponseEntity<ApprovalRequestResponseDto> 객체
     */
    private ResponseEntity<ApprovalRequestResponseDto> buildCreatedResponse(ApprovalRequestResponseDto responseDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
    }
}