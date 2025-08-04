package com.playdata.approvalservice.approval.service;

import com.playdata.approvalservice.approval.dto.AbsenceApprovalRequestCreateDto;
import com.playdata.approvalservice.approval.dto.AbsenceApprovalStatisticsDto;
import com.playdata.approvalservice.approval.dto.ApprovalRejectRequestDto;
import com.playdata.approvalservice.approval.dto.ApprovalRequestCreateDto;
import com.playdata.approvalservice.approval.dto.ApprovalRequestResponseDto;
import com.playdata.approvalservice.approval.dto.VacationApprovalRequestCreateDto;
import com.playdata.approvalservice.approval.entity.RequestType;
import com.playdata.approvalservice.common.auth.TokenUserInfo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface ApprovalService {

    @Transactional
    ApprovalRequestResponseDto createApprovalRequest(TokenUserInfo userInfo, ApprovalRequestCreateDto createDto);

    @Transactional
    ApprovalRequestResponseDto createVacationApprovalRequest(TokenUserInfo userInfo, VacationApprovalRequestCreateDto createDto);

    @Transactional
    ApprovalRequestResponseDto createAbsenceApprovalRequest(TokenUserInfo userInfo, AbsenceApprovalRequestCreateDto createDto);

    ApprovalRequestResponseDto getApprovalRequestById(Long id);

    List<ApprovalRequestResponseDto> getApprovalRequests(Long applicantId, String status, String requestType);

    @Transactional(readOnly = true)
    List<ApprovalRequestResponseDto> getAllApprovalRequests(RequestType requestType);

    @Transactional(readOnly = true)
    List<ApprovalRequestResponseDto> getPendingApprovalRequests(TokenUserInfo userInfo);

    @Transactional(readOnly = true)
    List<ApprovalRequestResponseDto> getProcessedApprovalRequestsByApproverId(TokenUserInfo userInfo);

    @Transactional
    ApprovalRequestResponseDto approveApprovalRequest(Long id, Long employeeNo);

    @Transactional
    ApprovalRequestResponseDto rejectApprovalRequest(Long id, TokenUserInfo userInfo, ApprovalRejectRequestDto rejectRequestDto);

    @Transactional
    ApprovalRequestResponseDto approveAbsenceApprovalRequest(Long id, Long employeeNo);

    @Transactional
    ApprovalRequestResponseDto rejectAbsenceApprovalRequest(Long id, TokenUserInfo userInfo, ApprovalRejectRequestDto rejectRequestDto);

    @Transactional(readOnly = true)
    AbsenceApprovalStatisticsDto getAbsenceApprovalStatistics();

    @Transactional(readOnly = true)
    Page<ApprovalRequestResponseDto> getAbsenceApprovalRequests(int page, int size);

    @Transactional(readOnly = true)
    Page<ApprovalRequestResponseDto> getPendingAbsenceApprovalRequests(TokenUserInfo userInfo, int page, int size);

    @Transactional(readOnly = true)
    Page<ApprovalRequestResponseDto> getProcessedAbsenceApprovalRequests(int page, int size);

    @Transactional(readOnly = true)
    Page<ApprovalRequestResponseDto> getMyAbsenceApprovalRequests(TokenUserInfo userInfo, int page, int size);

    @Transactional(readOnly = true)
    Page<ApprovalRequestResponseDto> getAbsenceApprovalRequestsProcessedByMe(TokenUserInfo userInfo, int page, int size);

    @Transactional
    ApprovalRequestResponseDto updateAbsenceApprovalRequest(Long id, com.playdata.approvalservice.client.dto.AbsenceApprovalRequestUpdateDto updateDto, TokenUserInfo userInfo);

    @Transactional
    void cancelApprovalRequest(Long id, TokenUserInfo userInfo);

    boolean hasApprovedLeave(Long userId, LocalDate date);

    String getApprovedLeaveType(Long userId, LocalDate date);
}