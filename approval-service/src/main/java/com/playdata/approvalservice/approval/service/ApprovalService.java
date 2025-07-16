package com.playdata.approvalservice.approval.service;

import com.playdata.approvalservice.client.HrServiceClient;
import com.playdata.approvalservice.client.dto.UserResDto;
import com.playdata.approvalservice.approval.dto.ApprovalRequestCreateDto;
import com.playdata.approvalservice.approval.dto.ApprovalRequestResponseDto;
import com.playdata.approvalservice.approval.entity.ApprovalRequest;
import com.playdata.approvalservice.approval.entity.ApprovalStatus;
import com.playdata.approvalservice.approval.entity.RequestType;
import com.playdata.approvalservice.approval.repository.ApprovalRepository;
import com.playdata.approvalservice.common.auth.TokenUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final HrServiceClient hrServiceClient;

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
                .title(createDto.getTitle())
                .vacationsId(createDto.getVacationsId())
                .vacationType(createDto.getVacationType())
                .certificatesId(createDto.getCertificatesId())
                .status(ApprovalStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        // 생성된 ApprovalRequest 엔티티를 데이터베이스에 저장합니다.
        ApprovalRequest savedRequest = approvalRepository.save(approvalRequest);

        // Feign Client 호출 전에 SecurityContextHolder에 인증 정보 설정
        // 이 로직은 FeignClientConfig에서 처리하므로 여기서는 불필요합니다.
        // Authentication originalAuth = SecurityContextHolder.getContext().getAuthentication();
        // try {
            // 저장된 엔티티를 응답 DTO로 변환하여 반환합니다。
            // 신청자 이름을 HR 서비스에서 가져와서 DTO에 포함
            String applicantName = hrServiceClient.getUsersInfo(List.of(savedRequest.getApplicantId()))
                    .stream().findFirst().map(UserResDto::getUserName).orElse("알 수 없음");

            return ApprovalRequestResponseDto.fromEntity(savedRequest, applicantName, null);
        // } finally {
            // Feign Client 호출 후 SecurityContextHolder 원상 복구
            // SecurityContextHolder.getContext().setAuthentication(originalAuth);
        // }
    }

    /**
     * ID를 통해 특정 승인 요청을 조회합니다.
     *
     * @param id 조회할 승인 요청의 ID
     * @return 조회된 승인 요청의 응답 DTO
     * @throws ResponseStatusException ID에 해당하는 승인 요청을 찾을 수 없을 경우 NOT_FOUND 상태 코드와 함께 예외 발생
     */
    public ApprovalRequestResponseDto getApprovalRequestById(Long id) {
        // ID를 사용하여 ApprovalRequest를 조회하고, 존재하지 않으면 예외를 발생시킵니다。
        ApprovalRequest approvalRequest = approvalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found with id: " + id));

        // 조회된 엔티티를 응답 DTO로 변환하여 반환합니다.
        // 신청자 이름과 결재자 이름을 HR 서비스에서 가져와서 DTO에 포함
        String applicantName = hrServiceClient.getUsersInfo(List.of(approvalRequest.getApplicantId()))
                .stream().findFirst().map(UserResDto::getUserName).orElse("알 수 없음");

        String approverName = null;
        if (approvalRequest.getApproverId() != null) {
            approverName = hrServiceClient.getUsersInfo(List.of(approvalRequest.getApproverId()))
                    .stream().findFirst().map(UserResDto::getUserName).orElse("알 수 없음");
        }

        return ApprovalRequestResponseDto.fromEntity(approvalRequest, applicantName, approverName);
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
    public ApprovalRequestResponseDto approveApprovalRequest(Long id, TokenUserInfo userInfo) {
        // ID를 사용하여 ApprovalRequest를 조회하고, 존재하지 않으면 예외를 발생시킵니다.
        ApprovalRequest approvalRequest = approvalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found with id: " + id));

        // 승인 요청이 PENDING 상태인지 확인합니다. 이미 승인되거나 반려된 요청은 처리할 수 없습니다.
        if (approvalRequest.getStatus() != ApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval request is not in PENDING status.");
        }

        // 승인자 ID를 설정하고, 엔티티의 approve 메소드를 호출하여 상태를 APPROVED로 변경합니다.
        approvalRequest.setApproverId(userInfo.getEmployeeNo());
        approvalRequest.approve();

        // 변경된 엔티티를 데이터베이스에 저장합니다.
        ApprovalRequest updatedRequest = approvalRepository.save(approvalRequest);

        // 업데이트된 엔티티를 응답 DTO로 변환하여 반환합니다.
        // 신청자 이름과 결재자 이름을 HR 서비스에서 가져와서 DTO에 포함
        String applicantName = hrServiceClient.getUsersInfo(List.of(updatedRequest.getApplicantId()))
                .stream().findFirst().map(UserResDto::getUserName).orElse("알 수 없음");

        String approverName = null;
        if (updatedRequest.getApproverId() != null) {
            approverName = hrServiceClient.getUsersInfo(List.of(updatedRequest.getApproverId()))
                    .stream().findFirst().map(UserResDto::getUserName).orElse("알 수 없음");
        }

        return ApprovalRequestResponseDto.fromEntity(updatedRequest, applicantName, approverName);
    }

    /**
     * 승인 요청을 반려 처리합니다.
     *
     * @param id 반려할 승인 요청의 ID
     * @param userInfo 인증된 사용자 정보
     * @return 반려 처리된 승인 요청의 응답 DTO
     * @throws ResponseStatusException ID에 해당하는 승인 요청을 찾을 수 없거나, 이미 처리된 요청일 경우 예외 발생
     */
    @Transactional
    public ApprovalRequestResponseDto rejectApprovalRequest(Long id, TokenUserInfo userInfo) {
        // ID를 사용하여 ApprovalRequest를 조회하고, 존재하지 않으면 예외를 발생시킵니다.
        ApprovalRequest approvalRequest = approvalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found with id: " + id));

        // 승인 요청이 PENDING 상태인지 확인합니다. 이미 승인되거나 반려된 요청은 처리할 수 없습니다.
        if (approvalRequest.getStatus() != ApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval request is not in PENDING status.");
        }

        // 반려자 ID를 설정하고, 엔티티의 reject 메소드를 호출하여 상태를 REJECTED로 변경합니다.
        approvalRequest.setApproverId(userInfo.getEmployeeNo());
        approvalRequest.reject();

        // 변경된 엔티티를 데이터베이스에 저장합니다.
        ApprovalRequest updatedRequest = approvalRepository.save(approvalRequest);

        // 업데이트된 엔티티를 응답 DTO로 변환하여 반환합니다.
        // 신청자 이름과 결재자 이름을 HR 서비스에서 가져와서 DTO에 포함
        String applicantName = hrServiceClient.getUsersInfo(List.of(updatedRequest.getApplicantId()))
                .stream().findFirst().map(UserResDto::getUserName).orElse("알 수 없음");

        String approverName = null;
        if (updatedRequest.getApproverId() != null) {
            approverName = hrServiceClient.getUsersInfo(List.of(updatedRequest.getApproverId()))
                    .stream().findFirst().map(UserResDto::getUserName).orElse("알 수 없음");
        }

        return ApprovalRequestResponseDto.fromEntity(updatedRequest, applicantName, approverName);
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

    /**
     * 모든 결재 요청 목록을 조회합니다.
     * 각 요청에 대한 신청자 및 결재자 정보를 HR 서비스로부터 가져와 함께 반환합니다.
     *
     * @return 모든 결재 요청 DTO 목록
     */
    @Transactional(readOnly = true)
    public List<ApprovalRequestResponseDto> getAllApprovalRequests() {
        List<ApprovalRequest> allRequests = approvalRepository.findAll();

        if (allRequests.isEmpty()) {
            return List.of(); // Java 9+ List.of() 사용
        }

        // 모든 신청자 및 결재자 ID 추출
        List<Long> allUserIds = allRequests.stream()
                .map(ApprovalRequest::getApplicantId)
                .collect(Collectors.toList());
        allRequests.stream()
                .filter(req -> req.getApproverId() != null)
                .map(ApprovalRequest::getApproverId)
                .forEach(allUserIds::add);

        // 중복 제거
        allUserIds = allUserIds.stream().distinct().collect(Collectors.toList());

        // HR 서비스에서 모든 사용자 정보 조회
        List<UserResDto> usersInfo;
        try {
            usersInfo = hrServiceClient.getUsersInfo(allUserIds);
            if (usersInfo == null || usersInfo.isEmpty()) {
                // 사용자 정보를 가져오지 못했으므로, 이름 없이 DTO 반환
                return allRequests.stream()
                        .map(req -> ApprovalRequestResponseDto.fromEntity(req, "알 수 없음", "알 수 없음"))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) { // FeignException 대신 일반 Exception으로 변경
            log.error("HR 서비스 통신 오류 (getAllApprovalRequests): {}", e.getMessage());
            // 통신 오류 시에도 이름 없이 DTO 반환
            return allRequests.stream()
                    .map(req -> ApprovalRequestResponseDto.fromEntity(req, "통신 오류", "통신 오류"))
                    .collect(Collectors.toList());
        }

        // 사용자 ID를 키로 하는 맵 생성 (빠른 조회를 위해)
        Map<Long, UserResDto> userMap = usersInfo.stream()
                .collect(Collectors.toMap(UserResDto::getEmployeeNo, Function.identity()));

        // ApprovalRequestResponseDto로 변환
        return allRequests.stream()
                .map(req -> {
                    String applicantName = Optional.ofNullable(userMap.get(req.getApplicantId()))
                            .map(UserResDto::getUserName).orElse("알 수 없음");
                    String approverName = Optional.ofNullable(req.getApproverId())
                            .map(userMap::get)
                            .map(UserResDto::getUserName).orElse(null);
                    return ApprovalRequestResponseDto.fromEntity(req, applicantName, approverName);
                })
                .collect(Collectors.toList());
    }
}