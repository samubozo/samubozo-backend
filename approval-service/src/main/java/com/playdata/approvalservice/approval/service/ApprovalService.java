package com.playdata.approvalservice.approval.service;

import com.playdata.approvalservice.approval.dto.ApprovalRequestCreateDto;
import com.playdata.approvalservice.approval.dto.ApprovalRequestResponseDto;
import com.playdata.approvalservice.approval.entity.ApprovalRequest;
import com.playdata.approvalservice.approval.entity.ApprovalStatus;
import com.playdata.approvalservice.approval.entity.RequestType; // RequestType 임포트 추가
import com.playdata.approvalservice.approval.repository.ApprovalRepository;
import com.playdata.approvalservice.common.auth.TokenUserInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate; // LocalDate 임포트 추가
import java.time.LocalDateTime;
import java.util.Optional; // Optional 임포트 추가

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalService {

    private final ApprovalRepository approvalRepository;

    /**
     * 새로운 승인 요청을 생성합니다.
     *
     * @param userInfo
     * @param createDto 승인 요청 생성에 필요한 데이터를 담은 DTO
     * @return 생성된 승인 요청의 응답 DTO
     */
    @Transactional
    public ApprovalRequestResponseDto createApprovalRequest(TokenUserInfo userInfo, ApprovalRequestCreateDto createDto) {

        // 보안 검증: 요청을 보낸 사용자의 employeeNo와 신청자 ID가 일치하는지 확인
        if (!userInfo.getEmployeeNo().equals(createDto.getApplicantId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user ID does not match the applicant ID in the request.");
        }
        // ApprovalRequest 엔티티를 빌더 패턴을 사용하여 생성합니다.
        // 초기 상태는 PENDING으로 설정하고, 요청 시간은 현재 시간으로 설정합니다.
        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .requestType(createDto.getRequestType())
                .applicantId(createDto.getApplicantId())
                .reason(createDto.getReason())
                .vacationsId(createDto.getVacationsId())
                .vacationType(createDto.getVacationType()) // vacationType 저장
                .certificatesId(createDto.getCertificateId())
                .status(ApprovalStatus.PENDING) // 초기 상태는 PENDING
                .requestedAt(LocalDateTime.now()) // 요청 시간은 현재 시간
                .build();

        // 생성된 ApprovalRequest 엔티티를 데이터베이스에 저장합니다.
        ApprovalRequest savedRequest = approvalRepository.save(approvalRequest);

        // 저장된 엔티티를 응답 DTO로 변환하여 반환합니다.
        return ApprovalRequestResponseDto.fromEntity(savedRequest);
    }

    /**
     * ID를 통해 특정 승인 요청을 조회합니다.
     *
     * @param id 조회할 승인 요청의 ID
     * @return 조회된 승인 요청의 응답 DTO
     * @throws ResponseStatusException ID에 해당하는 승인 요청을 찾을 수 없을 경우 NOT_FOUND 상태 코드와 함께 예외 발생
     */
    public ApprovalRequestResponseDto getApprovalRequestById(Long id) {
        // ID를 사용하여 ApprovalRequest를 조회하고, 존재하지 않으면 예외를 발생시킵니다.
        ApprovalRequest approvalRequest = approvalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found with id: " + id));

        // 조회된 엔티티를 응답 DTO로 변환하여 반환합니다.
        return ApprovalRequestResponseDto.fromEntity(approvalRequest);
    }

    /**
     * 승인 요청을 승인 처리합니다.
     *
     * @param id 승인할 승인 요청의 ID
     * @param approverId 승인자의 ID
     * @return 승인 처리된 승인 요청의 응답 DTO
     * @throws ResponseStatusException ID에 해당하는 승인 요청을 찾을 수 없거나, 이미 처리된 요청일 경우 예외 발생
     */
    @Transactional
    public ApprovalRequestResponseDto approveApprovalRequest(Long id, Long approverId) {
        // ID를 사용하여 ApprovalRequest를 조회하고, 존재하지 않으면 예외를 발생시킵니다.
        ApprovalRequest approvalRequest = approvalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found with id: " + id));

        // 승인 요청이 PENDING 상태인지 확인합니다. 이미 승인되거나 반려된 요청은 처리할 수 없습니다.
        if (approvalRequest.getStatus() != ApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval request is not in PENDING status.");
        }

        // 승인자 ID를 설정하고, 엔티티의 approve 메소드를 호출하여 상태를 APPROVED로 변경합니다.
        approvalRequest.setApproverId(approverId); // approverId 필드에 대한 setter가 필요합니다.
        approvalRequest.approve();

        // 변경된 엔티티를 데이터베이스에 저장합니다. (Transactional 어노테이션으로 인해 자동 저장될 수 있지만, 명시적으로 호출)
        ApprovalRequest updatedRequest = approvalRepository.save(approvalRequest);

        // 업데이트된 엔티티를 응답 DTO로 변환하여 반환합니다.
        return ApprovalRequestResponseDto.fromEntity(updatedRequest);
    }

    /**
     * 승인 요청을 반려 처리합니다.
     *
     * @param id 반려할 승인 요청의 ID
     * @param approverId 반려자의 ID
     * @return 반려 처리된 승인 요청의 응답 DTO
     * @throws ResponseStatusException ID에 해당하는 승인 요청을 찾을 수 없거나, 이미 처리된 요청일 경우 예외 발생
     */
    @Transactional
    public ApprovalRequestResponseDto rejectApprovalRequest(Long id, Long approverId) {
        // ID를 사용하여 ApprovalRequest를 조회하고, 존재하지 않으면 예외를 발생시킵니다.
        ApprovalRequest approvalRequest = approvalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found with id: " + id));

        // 승인 요청이 PENDING 상태인지 확인합니다. 이미 승인되거나 반려된 요청은 처리할 수 없습니다.
        if (approvalRequest.getStatus() != ApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval request is not in PENDING status.");
        }

        // 반려자 ID를 설정하고, 엔티티의 reject 메소드를 호출하여 상태를 REJECTED로 변경합니다.
        approvalRequest.setApproverId(approverId); // approverId 필드에 대한 setter가 필요합니다.
        approvalRequest.reject();

        // 변경된 엔티티를 데이터베이스에 저장합니다.
        ApprovalRequest updatedRequest = approvalRepository.save(approvalRequest);

        // 업데이트된 엔티티를 응답 DTO로 변환하여 반환합니다.
        return ApprovalRequestResponseDto.fromEntity(updatedRequest);
    }

    /**
     * 특정 사용자가 특정 날짜에 승인된 휴가(연차, 반차, 조퇴 등)가 있는지 확인합니다.
     *
     * @param userId 확인할 사용자의 ID
     * @param date 확인할 날짜
     * @return 승인된 휴가가 있으면 true, 없으면 false
     */
    public boolean hasApprovedLeave(Long userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59, 999999999); // 해당 날짜의 마지막 시간

        Optional<ApprovalRequest> approvedLeave = approvalRepository.findByApplicantIdAndRequestedAtBetweenAndStatus(
                userId, startOfDay, endOfDay, ApprovalStatus.APPROVED);

        return approvedLeave.isPresent();
    }

    /**
     * 특정 사용자가 특정 날짜에 승인된 휴가의 종류를 조회합니다.
     *
     * @param userId 확인할 사용자의 ID
     * @param date 확인할 날짜
     * @return 승인된 휴가 종류 (예: "HALF_DAY_LEAVE", "ANNUAL_LEAVE"), 없으면 null
     */
    public String getApprovedLeaveType(Long userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59, 999999999); // 해당 날짜의 마지막 시간

        Optional<ApprovalRequest> approvedLeave = approvalRepository.findByApplicantIdAndRequestedAtBetweenAndStatus(
                userId, startOfDay, endOfDay, ApprovalStatus.APPROVED);

        return approvedLeave
                .filter(request -> request.getRequestType() == RequestType.VACATION)
                .map(ApprovalRequest::getVacationType)
                .orElse(null);
    }
}
