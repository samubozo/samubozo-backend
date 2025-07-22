package com.playdata.approvalservice.approval.service;

import com.playdata.approvalservice.client.HrServiceClient;
import com.playdata.approvalservice.client.VacationServiceClient; // 추가
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

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
    private final VacationServiceClient vacationServiceClient; // 추가

    /**
     * 새로운 승인 요청을 생성합니다.
     *
     * @param userInfo
     * @param createDto 승인 요청 생성에 필요한 데이터를 담은 DTO
     * @return 생성된 승인 요청의 응답 DTO
     */
    @Transactional
    @CacheEvict(value = "approvalRequests", allEntries = true)
    public ApprovalRequestResponseDto createApprovalRequest(TokenUserInfo userInfo, ApprovalRequestCreateDto createDto) {
        log.info("createApprovalRequest 메서드 진입. userInfo: {}, createDto: {}", userInfo, createDto);

        // 보안 검증: 요청을 보낸 사용자의 employeeNo와 신청자 ID가 일치하는지 확인
        if (!userInfo.getEmployeeNo().equals(createDto.getApplicantId())) {
            log.warn("보안 검증 실패: 인증된 사용자 ID({})와 신청자 ID({})가 일치하지 않습니다.", userInfo.getEmployeeNo(), createDto.getApplicantId());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Authenticated user ID does not match the applicant ID in the request.");
        }

        // 중복 신청 방지: 동일한 신청자가 동일한 날짜에 PENDING 또는 APPROVED 상태의 요청을 가지고 있는지 확인
        LocalDateTime startOfDay = createDto.getStartDate().atStartOfDay();
        LocalDateTime endOfDay = createDto.getEndDate().atTime(23, 59, 59, 999999999);
        List<ApprovalRequest> existingRequests = approvalRepository.findByApplicantIdAndRequestedAtBetweenAndStatusIn(
                createDto.getApplicantId(), startOfDay, endOfDay, List.of(ApprovalStatus.PENDING, ApprovalStatus.APPROVED));

        if (!existingRequests.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 해당 기간에 처리 중이거나 승인된 결재 요청이 존재합니다.");
        }

        log.info("보안 검증 통과. ApprovalRequest 엔티티 빌드 시작.");

        // ApprovalRequest 엔티티를 빌더 패턴을 사용하여 생성합니다.
        // 초기 상태는 PENDING으로 설정하고, 요청 시간은 현재 시간으로 설정합니다.
        ApprovalRequest approvalRequest = ApprovalRequest.builder()
                .requestType(createDto.getRequestType())
                .applicantId(createDto.getApplicantId())
                .reason(createDto.getReason())
                .title(createDto.getTitle())
                .vacationsId(createDto.getVacationsId())
                .vacationType(createDto.getVacationType())
                .certificatesId(createDto.getCertificateId())
                .startDate(createDto.getStartDate()) // 추가
                .endDate(createDto.getEndDate()) // 추가
                .status(ApprovalStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .build();

        log.info("ApprovalRequest 엔티티 빌드 완료: {}", approvalRequest);
        log.info("ApprovalRequest 엔티티 저장 시도...");
        // 생성된 ApprovalRequest 엔티티를 데이터베이스에 저장합니다.
        ApprovalRequest savedRequest = null;
        try {
            savedRequest = approvalRepository.save(approvalRequest);
            log.info("ApprovalRequest 엔티티 저장 성공. 저장된 엔티티 ID: {}", savedRequest.getId());
        } catch (Exception e) {
            log.error("ApprovalRequest 엔티티 저장 실패: {}", e.getMessage(), e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "승인 요청 저장 중 오류 발생", e);
        }


        // 저장된 엔티티를 응답 DTO로 변환하여 반환합니다。
        // 신청자 이름을 HR 서비스에서 가져와서 DTO에 포함
        String applicantName = hrServiceClient.getUsersInfo(List.of(savedRequest.getApplicantId()))
                .stream().findFirst().map(UserResDto::getUserName).orElse("알 수 없음");

        ApprovalRequestResponseDto responseDto = ApprovalRequestResponseDto.fromEntity(savedRequest, applicantName, null);
        log.info("createApprovalRequest 메서드 종료. 응답 DTO: {}", responseDto);
        return responseDto;
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
        // 신청자 이름을 HR 서비스에서 가져와서 DTO에 포함
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
    @CacheEvict(value = "approvalRequests", allEntries = true)
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
        log.info("결재 요청 승인 후 상태: {}", approvalRequest.getStatus());

        // 변경된 엔티티를 데이터베이스에 저장합니다.
        ApprovalRequest updatedRequest = approvalRepository.save(approvalRequest);

        // 휴가 요청인 경우 vacation-service에 연차 차감 요청
        if (updatedRequest.getRequestType() == RequestType.VACATION && updatedRequest.getVacationsId() != null) {
            try {
                vacationServiceClient.updateVacationBalanceOnApproval(
                        updatedRequest.getVacationsId(),
                        ApprovalStatus.APPROVED.name(),
                        updatedRequest.getApplicantId(),
                        updatedRequest.getVacationType(), // String 그대로 전달
                        updatedRequest.getStartDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
                        updatedRequest.getEndDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                );
            } catch (Exception e) {
                log.error("VacationService 연차 차감 요청 실패: {}", e.getMessage());
                // 연차 차감 실패 시 결재 상태를 되돌리거나 롤백 처리 필요 (트랜잭션 관리)
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "연차 차감 처리 중 오류 발생");
            }
        }

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
    @CacheEvict(value = "approvalRequests", allEntries = true)
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
        log.info("결재 요청 반려 후 상태: {}", approvalRequest.getStatus());

        // 변경된 엔티티를 데이터베이스에 저장합니다.
        ApprovalRequest updatedRequest = approvalRepository.save(approvalRequest);

        // 휴가 요청인 경우 vacation-service에 연차 복구 요청
        if (updatedRequest.getRequestType() == RequestType.VACATION && updatedRequest.getVacationsId() != null) {
            try {
                vacationServiceClient.updateVacationBalanceOnApproval(
                        updatedRequest.getVacationsId(),
                        ApprovalStatus.REJECTED.name(),
                        updatedRequest.getApplicantId(),
                        updatedRequest.getVacationType(),
                        updatedRequest.getStartDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
                        updatedRequest.getEndDate().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                );
            } catch (Exception e) {
                log.error("VacationService 연차 복구 요청 실패: {}", e.getMessage());
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "연차 복구 처리 중 오류 발생");
            }
        }

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

        List<ApprovalRequest> approvedLeaves = approvalRepository.findAllByApplicantIdAndRequestedAtBetweenAndStatus(
                userId, startOfDay, endOfDay, ApprovalStatus.APPROVED);

        return !approvedLeaves.isEmpty();
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

        List<ApprovalRequest> approvedLeaves = approvalRepository.findAllByApplicantIdAndRequestedAtBetweenAndStatus(
                userId, startOfDay, endOfDay, ApprovalStatus.APPROVED);

        // 여러 개의 승인된 휴가가 있을 수 있으므로, 첫 번째 휴가의 종류를 반환
        return approvedLeaves.stream()
                .filter(request -> request.getRequestType() == RequestType.VACATION)
                .map(ApprovalRequest::getVacationType)
                .findFirst()
                .orElse(null);
    }

    /**
     * 모든 결재 요청 목록을 조회합니다.
     * 각 요청에 대한 신청자 및 결재자 정보를 HR 서비스로부터 가져와 함께 반환합니다.
     *
     * @return 모든 결재 요청 DTO 목록
     */
    @Transactional(readOnly = true)
    @Cacheable("approvalRequests")
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

    /**
     * 결재 대기 중인 모든 결재 요청 목록을 조회합니다. (hrRole='Y' 사용자용)
     * 각 요청에 대한 신청자 정보를 HR 서비스로부터 가져와 함께 반환합니다.
     *
     * @param userInfo 인증된 사용자 정보
     * @return 결재 대기 중인 결재 요청 DTO 목록
     * @throws ResponseStatusException hrRole이 'Y'가 아닌 경우 FORBIDDEN 상태 코드와 함께 예외 발생
     */
    @Transactional(readOnly = true)
    public List<ApprovalRequestResponseDto> getPendingApprovalRequests(TokenUserInfo userInfo) {
        // hrRole이 'Y'인지 확인
        if (!"Y".equals(userInfo.getHrRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only users with hrRole 'Y' can view pending approval requests.");
        }

        List<ApprovalRequest> pendingRequests = approvalRepository.findByStatus(ApprovalStatus.PENDING);

        if (pendingRequests.isEmpty()) {
            return List.of();
        }

        // 모든 신청자 ID 추출
        List<Long> applicantIds = pendingRequests.stream()
                .map(ApprovalRequest::getApplicantId)
                .distinct()
                .collect(Collectors.toList());

        // HR 서비스에서 신청자 정보 조회
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

        // 사용자 ID를 키로 하는 맵 생성
        Map<Long, UserResDto> userMap = usersInfo.stream()
                .collect(Collectors.toMap(UserResDto::getEmployeeNo, Function.identity()));

        // ApprovalRequestResponseDto로 변환
        return pendingRequests.stream()
                .map(req -> {
                    String applicantName = Optional.ofNullable(userMap.get(req.getApplicantId()))
                            .map(UserResDto::getUserName).orElse("알 수 없음");
                    return ApprovalRequestResponseDto.fromEntity(req, applicantName, null);
                })
                .collect(Collectors.toList());
    }

    /**
     * 특정 결재자가 처리한 (승인 또는 반려) 모든 결재 요청 목록을 조회합니다.
     * 각 요청에 대한 신청자 및 결재자 정보를 HR 서비스로부터 가져와 함께 반환합니다.
     *
     * @param userInfo 인증된 사용자 정보 (결재자 ID 추출용)
     * @return 처리된 결재 요청 DTO 목록
     * @throws ResponseStatusException hrRole이 'Y'가 아닌 경우 FORBIDDEN 상태 코드와 함께 예외 발생
     */
    @Transactional(readOnly = true)
    public List<ApprovalRequestResponseDto> getProcessedApprovalRequestsByApproverId(TokenUserInfo userInfo) {
        // hrRole이 'Y'인지 확인
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

        // 모든 신청자 및 결재자 ID 추출
        List<Long> allUserIds = processedRequests.stream()
                .map(ApprovalRequest::getApplicantId)
                .collect(Collectors.toList());
        allUserIds.add(approverId); // 결재자 본인 ID 추가

        // 중복 제거
        allUserIds = allUserIds.stream().distinct().collect(Collectors.toList());

        // HR 서비스에서 모든 사용자 정보 조회
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

        // 사용자 ID를 키로 하는 맵 생성
        Map<Long, UserResDto> userMap = usersInfo.stream()
                .collect(Collectors.toMap(UserResDto::getEmployeeNo, Function.identity()));

        // ApprovalRequestResponseDto로 변환
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
}