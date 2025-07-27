package com.playdata.approvalservice.approval.service;

import com.playdata.approvalservice.client.CertificateServiceClient;
import com.playdata.approvalservice.client.HrServiceClient;
import com.playdata.approvalservice.client.VacationServiceClient;
import com.playdata.approvalservice.client.dto.UserResDto;
import com.playdata.approvalservice.approval.dto.ApprovalRejectRequestDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalService {

    private final ApprovalRepository approvalRepository;
    private final HrServiceClient hrServiceClient;
    private final VacationServiceClient vacationServiceClient;
    private final CertificateServiceClient certificateServiceClient;

    @Transactional
    @CacheEvict(value = "approvalRequests", allEntries = true)
    public ApprovalRequestResponseDto createApprovalRequest(TokenUserInfo userInfo, ApprovalRequestCreateDto createDto) {
        log.info("createApprovalRequest 메서드 진입. userInfo: {}, createDto: {}", userInfo, createDto);

        if (!userInfo.getEmployeeNo().equals(createDto.getApplicantId())) {
            log.warn("보안 검증 실패: 인증된 사용자 ID({})와 신청자 ID({})가 일치하지 않습니다.", userInfo.getEmployeeNo(), createDto.getApplicantId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user ID does not match the applicant ID in the request.");
        }

        LocalDateTime startOfDay = createDto.getStartDate().atStartOfDay();
        LocalDateTime endOfDay = createDto.getEndDate().atTime(23, 59, 59, 999999999);
        List<ApprovalRequest> existingRequests = approvalRepository.findByApplicantIdAndRequestTypeAndRequestedAtBetweenAndStatusIn(
                createDto.getApplicantId(), createDto.getRequestType(), startOfDay, endOfDay, List.of(ApprovalStatus.PENDING, ApprovalStatus.APPROVED));

        if (!existingRequests.isEmpty()) {
            if (createDto.getRequestType() == RequestType.CERTIFICATE) {
                // 증명서 요청인 경우, 기존 요청의 증명서 유형과 현재 요청의 증명서 유형을 비교
                for (ApprovalRequest existingRequest : existingRequests) {
                    if (existingRequest.getCertificateId() != null) {
                        try {
                            // certificate-service에서 증명서 상세 정보 조회
                            com.playdata.approvalservice.client.dto.Certificate certificate = certificateServiceClient.getCertificateById(existingRequest.getCertificateId());
                            if (certificate != null && certificate.getType() != null && certificate.getType().equals(createDto.getCertificateType())) {
                                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        String.format("이미 해당 기간에 동일한 유형 (%s)의 증명서 신청이 존재합니다.", createDto.getCertificateType()));
                            }
                        } catch (feign.FeignException e) {
                            if (e.status() == HttpStatus.NOT_FOUND.value()) {
                                log.warn("CertificateService에서 증명서를 찾을 수 없습니다 (certificateId: {}). 증명서가 아직 생성되지 않았을 수 있습니다. 중복 검사를 건너뜁니다.", existingRequest.getCertificateId());
                                // 404 에러는 무시하고 다음 로직으로 진행
                                continue;
                            }
                            log.error("CertificateService 통신 오류 (getCertificateById) for certificateId {}: {}", existingRequest.getCertificateId(), e.getMessage());
                            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "증명서 정보 조회 중 오류 발생", e);
                        }
                    }
                }
            } else {
                // 휴가 등 다른 요청 유형은 기존 중복 검증 메시지 유지
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 해당 기간에 처리 중이거나 승인된 결재 요청이 존재합니다.");
            }
        }

        log.info("보안 검증 통과. ApprovalRequest 엔티티 빌드 시작.");

        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .requestType(createDto.getRequestType())
                .applicantId(createDto.getApplicantId())
                .reason(createDto.getReason())
                .title(createDto.getTitle())
                .vacationsId(createDto.getVacationsId())
                .vacationType(createDto.getVacationType())
                .certificatesId(createDto.getCertificateId())
                .startDate(createDto.getStartDate())
                .endDate(createDto.getEndDate())
                .status(ApprovalStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        log.info("ApprovalRequest 엔티티 빌드 완료: {}", approvalRequest);
        log.info("ApprovalRequest 엔티티 저장 시도...");
        ApprovalRequest savedRequest = null;
        try {
            savedRequest = approvalRepository.save(approvalRequest);
            log.info("ApprovalRequest 엔티티 저장 성공. 저장된 엔티티 ID: {}", savedRequest.getId());
        } catch (Exception e) {
            log.error("ApprovalRequest 엔티티 저장 실패: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "승인 요청 저장 중 오류 발생", e);
        }

        String applicantName = hrServiceClient.getUsersInfo(List.of(savedRequest.getApplicantId()))
                .stream().findFirst().map(UserResDto::getUserName).orElse("알 수 없음");

        ApprovalRequestResponseDto responseDto = ApprovalRequestResponseDto.fromEntity(savedRequest, applicantName, null);
        log.info("createApprovalRequest 메서드 종료. 응답 DTO: {}", responseDto);
        return responseDto;
    }

    public ApprovalRequestResponseDto getApprovalRequestById(Long id) {
        ApprovalRequest approvalRequest = approvalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found with id: " + id));
        log.info("Retrieved ApprovalRequest approverId: {}", approvalRequest.getApproverId());

        String applicantName = hrServiceClient.getUsersInfo(List.of(approvalRequest.getApplicantId()))
                .stream().findFirst().map(UserResDto::getUserName).orElse("알 수 없음");

        String approverName = null;
        if (approvalRequest.getApproverId() != null) {
            log.info("Attempting to get approver name for approverId: {}", approvalRequest.getApproverId());
            approverName = hrServiceClient.getUsersInfo(List.of(approvalRequest.getApproverId()))
                    .stream().findFirst().map(UserResDto::getUserName).orElse("알 수 없음");
            log.info("Approver name retrieved: {}", approverName);
        }

        return ApprovalRequestResponseDto.fromEntity(approvalRequest, applicantName, approverName);
    }

    @Transactional
    @CacheEvict(value = "approvalRequests", allEntries = true)
    public ApprovalRequestResponseDto approveApprovalRequest(Long id, @RequestHeader("X-User-Employee-No") Long employeeNo) {
        log.info("1. approveApprovalRequest 메서드 시작. 요청 ID: {}, 승인자 EmployeeNo: {}", id, employeeNo); // 추가된 로그

        ApprovalRequest approvalRequest = approvalRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Approval request not found with id: {}", id); // 추가된 로그
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found with id: " + id);
                });
        log.info("2. 결재 요청 엔티티 조회 성공. 현재 상태: {}", approvalRequest.getStatus()); // 추가된 로그

        if (approvalRequest.getStatus() != ApprovalStatus.PENDING) {
            log.error("3. 결재 요청 상태 오류: PENDING 상태가 아님. 현재 상태: {}", approvalRequest.getStatus()); // 추가된 로그
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval request is not in PENDING status.");
        }
        log.info("4. 결재 요청 상태 PENDING 확인."); // 추가된 로그

        approvalRequest.setApproverId(employeeNo);
        log.info("5. Approver ID 설정 완료: {}", approvalRequest.getApproverId()); // 기존 로그 유지
        approvalRequest.approve();
        log.info("6. 결재 요청 승인 처리 완료. 상태: {}", approvalRequest.getStatus()); // 기존 로그 유지

        ApprovalRequest updatedRequest = approvalRepository.save(approvalRequest);
        log.info("7. ApprovalRequest 엔티티 저장 완료. 저장된 Approver ID: {}", updatedRequest.getApproverId()); // 기존 로그 유지

        String approverName = null; // 여기에 선언
        if (updatedRequest.getApproverId() != null) {
            log.info("16. 결재자 ID 존재. HR 서비스에서 결재자 이름 조회 시도. Approver ID: {}", updatedRequest.getApproverId());
            List<UserResDto> approverUsersInfo = hrServiceClient.getUsersInfo(List.of(updatedRequest.getApproverId()));
            log.info("17. HR 서비스로부터 받은 결재자 정보 (approverUsersInfo): {}", approverUsersInfo);
            approverName = approverUsersInfo.stream().findFirst().map(UserResDto::getUserName).orElse("알 수 없음");
            log.info("18. 설정된 approverName: {}", approverName);
        } else {
            log.warn("19. updatedRequest.getApproverId()가 null입니다. 결재자 이름을 가져올 수 없습니다.");
        }

        if (updatedRequest.getRequestType() == RequestType.VACATION && updatedRequest.getVacationsId() != null) {
            log.info("8. 휴가 요청 처리 시작.");
            try {
                vacationServiceClient.updateVacationBalanceOnApproval(
                        updatedRequest.getVacationsId(),
                        ApprovalStatus.APPROVED.name(),
                        updatedRequest.getApplicantId(),
                        updatedRequest.getVacationType(),
                        updatedRequest.getStartDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
                        updatedRequest.getEndDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
                        null
                );
                log.info("9. VacationService 연차 차감 요청 성공.");
            } catch (Exception e) {
                log.error("10. VacationService 연차 차감 요청 실패: {}", e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "연차 차감 처리 중 오류 발생");
            }
        } else if (updatedRequest.getRequestType() == RequestType.CERTIFICATE && updatedRequest.getCertificateId() != null) {
            log.info("11. 증명서 요청 처리 시작.");
            try {
                certificateServiceClient.approveCertificate(updatedRequest.getCertificateId(), updatedRequest.getApproverId(), approverName);
                log.info("12. CertificateService 증명서 승인 요청 성공. certificateId: {}", updatedRequest.getCertificateId());
            } catch (Exception e) {
                log.error("13. CertificateService 증명서 승인 요청 실패: {}", e.getMessage(), e);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "증명서 승인 처리 중 오류 발생");
            }
        }
        log.info("14. 서비스별 후처리 완료."); // 추가된 로그

        String applicantName = hrServiceClient.getUsersInfo(List.of(updatedRequest.getApplicantId()))
                .stream().findFirst().map(UserResDto::getUserName).orElse("알 수 없음");
        log.info("15. 신청자 이름 조회 완료: {}", applicantName); // 추가된 로그

        log.info("20. 최종 approverId for response: {}, approverName for response: {}", updatedRequest.getApproverId(), approverName); // 기존 로그 유지

        return ApprovalRequestResponseDto.fromEntity(updatedRequest, applicantName, approverName);
    }

    @Transactional
    @CacheEvict(value = "approvalRequests", allEntries = true)
    public ApprovalRequestResponseDto rejectApprovalRequest(Long id, TokenUserInfo userInfo, ApprovalRejectRequestDto rejectRequestDto) {
        ApprovalRequest approvalRequest = approvalRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Approval request not found with id: " + id));

        if (approvalRequest.getStatus() != ApprovalStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Approval request is not in PENDING status.");
        }

        approvalRequest.setApproverId(userInfo.getEmployeeNo());
        approvalRequest.reject(rejectRequestDto.getRejectComment()); // 반려 및 사유 저장
        log.info("결재 요청 반려 후 상태: {}, 반려 사유: {}", approvalRequest.getStatus(), approvalRequest.getRejectComment());

        ApprovalRequest updatedRequest = approvalRepository.save(approvalRequest);

        String approverName = null;
        if (updatedRequest.getApproverId() != null) {
            approverName = hrServiceClient.getUsersInfo(List.of(updatedRequest.getApproverId()))
                    .stream().findFirst().map(UserResDto::getUserName).orElse("알 수 없음");
            log.info("설정된 approverName: {}", approverName);
        } else {
            log.warn("updatedRequest.getApproverId()가 null입니다. 결재자 이름을 가져올 수 없습니다.");
        }

        if (updatedRequest.getRequestType() == RequestType.VACATION && updatedRequest.getVacationsId() != null) {
            try {
                vacationServiceClient.updateVacationBalanceOnApproval(
                        updatedRequest.getVacationsId(),
                        ApprovalStatus.REJECTED.name(),
                        updatedRequest.getApplicantId(),
                        updatedRequest.getVacationType(),
                        updatedRequest.getStartDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
                        updatedRequest.getEndDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
                        updatedRequest.getRejectComment()
                );
            } catch (Exception e) {
                log.error("VacationService 연차 복구 요청 실패: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "연차 복구 처리 중 오류 발생");
            }
        } else if (updatedRequest.getRequestType() == RequestType.CERTIFICATE && updatedRequest.getCertificateId() != null) {
            try {
                certificateServiceClient.rejectCertificateInternal(updatedRequest.getCertificateId(), approverName);
                log.info("Successfully sent rejection to certificate-service for certificateId: {}", updatedRequest.getCertificateId());
            } catch (Exception e) {
                log.error("CertificateService 증명서 반려 요청 실패: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "증명서 반려 처리 중 오류 발생");
            }
        }

        String applicantName = hrServiceClient.getUsersInfo(List.of(updatedRequest.getApplicantId()))
                .stream().findFirst().map(UserResDto::getUserName).orElse("알 수 없음");

        log.info("설정된 approverName: {}", approverName);

        return ApprovalRequestResponseDto.fromEntity(updatedRequest, applicantName, approverName);
    }

    public boolean hasApprovedLeave(Long userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59, 999999999);

        List<ApprovalRequest> approvedLeaves = approvalRepository.findAllByApplicantIdAndRequestedAtBetweenAndStatus(
                userId, startOfDay, endOfDay, ApprovalStatus.APPROVED);

        return !approvedLeaves.isEmpty();
    }

    public String getApprovedLeaveType(Long userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59, 999999999);

        List<ApprovalRequest> approvedLeaves = approvalRepository.findAllByApplicantIdAndRequestedAtBetweenAndStatus(
                userId, startOfDay, endOfDay, ApprovalStatus.APPROVED);

        return approvedLeaves.stream()
                .filter(request -> request.getRequestType() == RequestType.VACATION)
                .map(ApprovalRequest::getVacationType)
                .findFirst()
                .orElse(null);
    }

    @Transactional(readOnly = true)
    @Cacheable("approvalRequests")
    public List<ApprovalRequestResponseDto> getAllApprovalRequests() {
        List<ApprovalRequest> allRequests = approvalRepository.findAll();

        if (allRequests.isEmpty()) {
            return List.of();
        }

        List<Long> allUserIds = allRequests.stream()
                .map(ApprovalRequest::getApplicantId)
                .collect(Collectors.toList());
        allRequests.stream()
                .filter(req -> req.getApproverId() != null)
                .map(ApprovalRequest::getApproverId)
                .forEach(allUserIds::add);

        allUserIds = allUserIds.stream().distinct().collect(Collectors.toList());

        List<UserResDto> usersInfo;
        try {
            usersInfo = hrServiceClient.getUsersInfo(allUserIds);
            if (usersInfo == null || usersInfo.isEmpty()) {
                return allRequests.stream()
                        .map(req -> ApprovalRequestResponseDto.fromEntity(req, "알 수 없음", "알 수 없음"))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("HR 서비스 통신 오류 (getAllApprovalRequests): {}", e.getMessage());
            return allRequests.stream()
                    .map(req -> ApprovalRequestResponseDto.fromEntity(req, "통신 오류", "통신 오류"))
                    .collect(Collectors.toList());
        }

        Map<Long, UserResDto> userMap = usersInfo.stream()
                .collect(Collectors.toMap(UserResDto::getEmployeeNo, Function.identity()));

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

    /**
     * 특정 요청 유형에 해당하는 모든 결재 요청을 조회합니다.
     *
     * @param requestType 조회할 결재 요청의 유형 (예: VACATION, CERTIFICATE)
     * @return 지정된 유형의 결재 요청 목록 (ApprovalRequestResponseDto 형태로 변환)
     */
    @Transactional(readOnly = true)
    @Cacheable(value = "approvalRequests", key = "#requestType")
    public List<ApprovalRequestResponseDto> getAllApprovalRequests(RequestType requestType) {
        log.info("getAllApprovalRequests 메서드 진입 (requestType 필터링). 요청 유형: {}", requestType);
        List<ApprovalRequest> filteredRequests = approvalRepository.findByRequestType(requestType);

        if (filteredRequests.isEmpty()) {
            log.info("요청 유형 {}에 해당하는 결재 요청이 없습니다.", requestType);
            return List.of();
        }

        List<Long> allUserIds = filteredRequests.stream()
                .map(ApprovalRequest::getApplicantId)
                .collect(Collectors.toList());
        filteredRequests.stream()
                .filter(req -> req.getApproverId() != null)
                .map(ApprovalRequest::getApproverId)
                .forEach(allUserIds::add);

        allUserIds = allUserIds.stream().distinct().collect(Collectors.toList());

        List<UserResDto> usersInfo;
        try {
            usersInfo = hrServiceClient.getUsersInfo(allUserIds);
            if (usersInfo == null || usersInfo.isEmpty()) {
                log.warn("HR 서비스로부터 사용자 정보를 가져오지 못했습니다. userIds: {}", allUserIds);
                return filteredRequests.stream()
                        .map(req -> ApprovalRequestResponseDto.fromEntity(req, "알 수 없음", "알 수 없음"))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("HR 서비스 통신 오류 (getAllApprovalRequests - requestType 필터링): {}", e.getMessage());
            return filteredRequests.stream()
                    .map(req -> ApprovalRequestResponseDto.fromEntity(req, "통신 오류", "통신 오류"))
                    .collect(Collectors.toList());
        }

        Map<Long, UserResDto> userMap = usersInfo.stream()
                .collect(Collectors.toMap(UserResDto::getEmployeeNo, Function.identity()));

        return filteredRequests.stream()
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

    @Transactional(readOnly = true)
    public List<ApprovalRequestResponseDto> getPendingApprovalRequests(TokenUserInfo userInfo) {
        if (!"Y".equals(userInfo.getHrRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only users with hrRole 'Y' can view pending approval requests.");
        }

        List<ApprovalRequest> pendingRequests = approvalRepository.findByStatus(ApprovalStatus.PENDING);

        if (pendingRequests.isEmpty()) {
            return List.of();
        }

        List<Long> applicantIds = pendingRequests.stream()
                .map(ApprovalRequest::getApplicantId)
                .distinct()
                .collect(Collectors.toList());

        List<UserResDto> usersInfo;
        try {
            usersInfo = hrServiceClient.getUsersInfo(applicantIds);
            if (usersInfo == null || usersInfo.isEmpty()) {
                log.warn("HR 서비스로부터 신청자 정보를 가져오지 못했습니다. applicantIds: {}", applicantIds);
                return pendingRequests.stream()
                        .map(req -> ApprovalRequestResponseDto.fromEntity(req, "알 수 없음", null))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("HR 서비스 통신 오류 (getPendingApprovalRequests): {}", e.getMessage());
            return pendingRequests.stream()
                    .map(req -> ApprovalRequestResponseDto.fromEntity(req, "통신 오류", null))
                    .collect(Collectors.toList());
        }

        Map<Long, UserResDto> userMap = usersInfo.stream()
                .collect(Collectors.toMap(UserResDto::getEmployeeNo, Function.identity()));

        return pendingRequests.stream()
                .map(req -> {
                    String applicantName = Optional.ofNullable(userMap.get(req.getApplicantId()))
                            .map(UserResDto::getUserName).orElse("알 수 없음");
                    return ApprovalRequestResponseDto.fromEntity(req, applicantName, null);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ApprovalRequestResponseDto> getProcessedApprovalRequestsByApproverId(TokenUserInfo userInfo) {
        if (!"Y".equals(userInfo.getHrRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only users with hrRole 'Y' can view processed approval requests.");
        }

        Long approverId = userInfo.getEmployeeNo();
        List<ApprovalRequest> processedRequests = approvalRepository.findByApproverIdAndStatusInOrderByProcessedAtDesc(
                approverId, List.of(ApprovalStatus.APPROVED, ApprovalStatus.REJECTED)
        );

        if (processedRequests.isEmpty()) {
            return List.of();
        }

        List<Long> allUserIds = processedRequests.stream()
                .map(ApprovalRequest::getApplicantId)
                .collect(Collectors.toList());
        allUserIds.add(approverId);

        allUserIds = allUserIds.stream().distinct().collect(Collectors.toList());

        List<UserResDto> usersInfo;
        try {
            usersInfo = hrServiceClient.getUsersInfo(allUserIds);
            if (usersInfo == null || usersInfo.isEmpty()) {
                log.warn("HR 서비스로부터 사용자 정보를 가져오지 못했습니다. userIds: {}", allUserIds);
                return processedRequests.stream()
                        .map(req -> ApprovalRequestResponseDto.fromEntity(req, "알 수 없음", "알 수 없음"))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.error("HR 서비스 통신 오류 (getProcessedApprovalRequestsByApproverId): {}", e.getMessage());
            return processedRequests.stream()
                    .map(req -> ApprovalRequestResponseDto.fromEntity(req, "통신 오류", "통신 오류"))
                    .collect(Collectors.toList());
        }

        Map<Long, UserResDto> userMap = usersInfo.stream()
                .collect(Collectors.toMap(UserResDto::getEmployeeNo, Function.identity()));

        return processedRequests.stream()
                .map(req -> {
                    String applicantName = Optional.ofNullable(userMap.get(req.getApplicantId()))
                            .map(UserResDto::getUserName).orElse("알 수 없음");
                    String approverName = Optional.ofNullable(userMap.get(req.getApproverId()))
                            .map(UserResDto::getUserName).orElse("알 수 없음");
                    return ApprovalRequestResponseDto.fromEntity(req, applicantName, approverName);
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public ApprovalRequestResponseDto createVacationApprovalRequest(TokenUserInfo userInfo, com.playdata.approvalservice.approval.dto.VacationApprovalRequestCreateDto createDto) {
        ApprovalRequestCreateDto generalDto = ApprovalRequestCreateDto.builder()
                .requestType(RequestType.VACATION)
                .applicantId(userInfo.getEmployeeNo())
                .title(createDto.getTitle())
                .reason(createDto.getReason())
                .vacationsId(createDto.getVacationsId())
                .vacationType(createDto.getVacationType())
                .startDate(createDto.getStartDate())
                .endDate(createDto.getEndDate())
                .build();
        return createApprovalRequest(userInfo, generalDto);
    }

    @Transactional
    public ApprovalRequestResponseDto createCertificateApprovalRequest(TokenUserInfo userInfo, com.playdata.approvalservice.approval.dto.CertificateApprovalRequestCreateDto createDto) {
        ApprovalRequestCreateDto generalDto = ApprovalRequestCreateDto.builder()
                .requestType(RequestType.CERTIFICATE)
                .applicantId(userInfo.getEmployeeNo())
                .title(createDto.getTitle())
                .reason(createDto.getReason())
                .certificateId(createDto.getCertificateId())
                .startDate(createDto.getStartDate())
                .endDate(createDto.getEndDate())
                .build();
        return createApprovalRequest(userInfo, generalDto);
    }
}
