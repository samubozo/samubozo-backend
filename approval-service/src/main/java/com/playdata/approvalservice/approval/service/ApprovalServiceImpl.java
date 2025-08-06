package com.playdata.approvalservice.approval.service;

import com.playdata.approvalservice.approval.dto.*;
import com.playdata.approvalservice.client.*;
import com.playdata.approvalservice.client.dto.*;
import com.playdata.approvalservice.approval.entity.*;
import com.playdata.approvalservice.approval.repository.ApprovalRepository;
import com.playdata.approvalservice.common.auth.TokenUserInfo;
import com.playdata.approvalservice.common.exception.ApprovalBadRequestException;
import com.playdata.approvalservice.common.exception.ApprovalConflictException;
import com.playdata.approvalservice.common.exception.ApprovalForbiddenException;
import com.playdata.approvalservice.common.exception.ApprovalInternalServerErrorException;
import com.playdata.approvalservice.common.exception.ApprovalNotFoundException;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.playdata.approvalservice.approval.repository.ApprovalSpecification;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 결재 요청 서비스 - 타입별로 명확히 분리된 구조로 리팩터링
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApprovalServiceImpl implements ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalServiceImpl.class);

    private final ApprovalRepository approvalRepository;
    private final HrServiceClient hrServiceClient;
    private final VacationServiceClient vacationServiceClient;
    private final CertificateServiceClient certificateServiceClient;
    private final AbsenceServiceClient absenceServiceClient;

    // ===== 공통 유틸리티 메서드들 =====

    /**
     * 사용자 정보를 일괄 조회하여 Map으로 반환
     */
    private Map<Long, UserResDto> getUserMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return new HashMap<>();
        }
        try {
            List<UserResDto> usersInfo = hrServiceClient.getUsersInfo(userIds);
            if (usersInfo == null || usersInfo.isEmpty()) {
                log.warn("HR 서비스로부터 사용자 정보를 가져오지 못했습니다. userIds: {}", userIds);
                return new HashMap<>();
            }
            return usersInfo.stream()
                    .collect(Collectors.toMap(UserResDto::getEmployeeNo, Function.identity()));
        } catch (Exception e) {
            log.error("HR 서비스 통신 오류 (getUserMap): {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * ApprovalRequestResponseDto를 빌드하는 공통 메서드
     */
    private ApprovalRequestResponseDto buildResponseDto(ApprovalRequest request, Map<Long, UserResDto> userMap) {
        String applicantName = Optional.ofNullable(userMap.get(request.getApplicantId()))
                .map(UserResDto::getUserName).orElse("알 수 없음");
        String applicantDepartment = Optional.ofNullable(userMap.get(request.getApplicantId()))
                .map(UserResDto::getDepartment)
                .map(DepartmentResDto::getName)
                .orElse("");
        String approverName = Optional.ofNullable(request.getApproverId())
                .map(userMap::get)
                .map(UserResDto::getUserName).orElse(null);

        return ApprovalRequestResponseDto.fromEntity(request, applicantName, approverName, applicantDepartment);
    }

    /**
     * 중복 검증을 수행하는 공통 메서드
     */
    private void validateDuplicateRequest(ApprovalRequestCreateDto createDto) {
        RequestType requestType = createDto.getRequestType();

        log.info("유효성 검증 시작: applicantId={}, requestType={}",
                createDto.getApplicantId(), requestType);

        // 요청 유형에 따라 각기 다른 검증 로직을 실행
        switch (requestType) {
            case VACATION: // 휴가
            case ABSENCE:  // 부재
                // 휴가와 부재는 기간 중복 검사를 수행합니다.
                validateDateOverlap(createDto);
                break;

            case CERTIFICATE: // 증명서
                // 증명서는 기간 중복 검사 대신, 증명서 고유의 중복 검사를 수행합니다.
                validateCertificateRequest(createDto);
                break;

            default:
                // 알려지지 않은 타입이거나 검증이 필요 없는 경우
                log.info("'{}' 유형은 별도의 중복 검사를 수행하지 않습니다.", requestType);
                break;
        }
    }

    /**
     * [신규 추가] 휴가/부재용 기간 중복 검증 헬퍼 메서드
     */
    private void validateDateOverlap(ApprovalRequestCreateDto createDto) {
        log.info("기간 중복 검사를 수행합니다. (휴가/부재)");
        List<ApprovalStatus> targetStatuses = List.of(ApprovalStatus.PENDING, ApprovalStatus.APPROVED);
        List<ApprovalRequest> existingRequests = approvalRepository.findOverlappingRequestsForUser(
                createDto.getApplicantId(),
                createDto.getStartDate(),
                createDto.getEndDate(),
                targetStatuses
        );

        if (!existingRequests.isEmpty()) {
            // [수정] requestedAt이 null일 경우를 대비하여 null-safe한 비교 로직으로 변경
            ApprovalRequest representativeRequest = existingRequests.stream()
                    .min(Comparator.comparing(ApprovalRequest::getRequestedAt, Comparator.nullsLast(Comparator.naturalOrder())))
                    .orElse(existingRequests.get(0)); // 만약의 경우 첫 번째 항목 사용

            String requestTypeKorean = "요청"; // 기본값
            if (representativeRequest.getRequestType() != null) {
                 requestTypeKorean = switch (representativeRequest.getRequestType()) {
                    case VACATION -> "휴가";
                    case ABSENCE -> "부재";
                    case CERTIFICATE -> "증명서";
                };
            }

            String errorMessage = String.format(
                    "해당 기간(%s ~ %s)은 이미 처리 중이거나 승인된 '%s' 요청과 중복됩니다.",
                    createDto.getStartDate(),
                    createDto.getEndDate(),
                    requestTypeKorean
            );
            throw new ApprovalConflictException(errorMessage);
        }
    }

    /**
     * [신규 추가] 증명서용 유효성 검증 헬퍼 메서드
     */
    private void validateCertificateRequest(ApprovalRequestCreateDto createDto) {
        log.info("증명서 중복 검사를 수행합니다. (증명서)");

        // 1. PENDING 상태의 동일 유형 증명서 요청 확인
        Optional<ApprovalRequest> existingPendingCertificateRequest = approvalRepository.findByApplicantIdAndRequestTypeAndCertificateTypeAndStatus(
                createDto.getApplicantId(),
                RequestType.CERTIFICATE,
                createDto.getCertificateType(),
                ApprovalStatus.PENDING
        );

        if (existingPendingCertificateRequest.isPresent()) {
            String errorMessage = String.format("이미 처리 대기 중인 동일한 유형(%s)의 증명서 신청이 존재합니다.",
                    createDto.getCertificateType());
            throw new ApprovalConflictException(errorMessage);
        }

        // 2. 유효 기간이 만료되지 않은 APPROVED 상태의 동일 유형 증명서 요청 확인 (certificate-service 호출)
        try {
            Boolean hasValidCertificate = certificateServiceClient.getValidCertificateInternal(
                    createDto.getApplicantId(),
                    createDto.getCertificateType()
            );

            if (Boolean.TRUE.equals(hasValidCertificate)) {
                String errorMessage = String.format("이미 유효한 동일한 유형(%s)의 증명서가 존재합니다. 다시 신청하시려면 기존 증명서의 만료일을 확인해주세요.",
                        createDto.getCertificateType());
                throw new ApprovalConflictException(errorMessage);
            }
        } catch (FeignException e) {
            log.error("CertificateService 통신 오류 (getValidCertificateInternal): {}", e.getMessage(), e);
            throw new ApprovalInternalServerErrorException("증명서 유효성 확인 중 오류 발생", e);
        }
    }


    /**
     * 승인자 ID를 설정하는 공통 메서드
     */
    private Long determineApproverId(ApprovalRequestCreateDto createDto) {
        if (createDto.getRequestType() == RequestType.CERTIFICATE) {
            return getHrApproverId();
        }
        return createDto.getApproverId();
    }

    /**
     * HR 승인자 ID를 조회
     */
    private Long getHrApproverId() {
        try {
            List<Long> hrUserIds = List.of(1L); // HR 담당자 ID
            List<UserResDto> hrUsers = hrServiceClient.getUsersInfo(hrUserIds);
            if (!hrUsers.isEmpty()) {
                log.info("HR 담당자 정보 설정 완료. ID: {}", hrUsers.get(0).getEmployeeNo());
                return hrUsers.get(0).getEmployeeNo();
            }
        } catch (Exception e) {
            log.warn("HR 담당자 정보 조회 중 오류 발생. 기본값을 사용합니다.", e);
        }
        return 1L; // 기본값
    }

    // ===== 결재 요청 생성 메서드들 =====

    /**
     * 일반적인 결재 요청 생성
     */
    @Override
    @Transactional
    @CacheEvict(value = "approvalRequests", allEntries = true)
    public ApprovalRequestResponseDto createApprovalRequest(TokenUserInfo userInfo, ApprovalRequestCreateDto createDto) {
        log.info("createApprovalRequest 메서드 진입. userInfo: {}, createDto: {}", userInfo, createDto);

        // 1. 보안 검증
        if (!userInfo.getEmployeeNo().equals(createDto.getApplicantId())) {
            log.warn("보안 검증 실패: 인증된 사용자 ID({})와 신청자 ID({})가 일치하지 않습니다.",
                    userInfo.getEmployeeNo(), createDto.getApplicantId());
            throw new ApprovalForbiddenException("인증된 사용자 ID와 신청자 ID가 일치하지 않습니다.");
        }
        log.info("--- 1. 보안 검증 통과 ---");

        // 2. 중복 검증 수행
        try {
            validateDuplicateRequest(createDto);
            log.info("--- 2. 유효성 검사 통과 ---");
        } catch (ApprovalConflictException e) {
            log.error("중복 검증 중 오류 발생: {}", e.getMessage());
            throw e; // 유효성 검증 실패 시 바로 예외를 던집니다.
        } catch (Exception e) {
            log.error("예상치 못한 중복 검증 오류 발생: {}", e.getMessage(), e);
            throw new ApprovalInternalServerErrorException("중복 검증 중 알 수 없는 오류 발생", e);
        }

        // 3. 승인자 ID 설정
        Long approverId = null;
        try {
            approverId = determineApproverId(createDto);
            log.info("--- 3. 승인자 ID 설정 완료: {}", approverId);
        } catch (Exception e) {
            log.error("승인자 ID 결정 중 오류 발생: {}", e.getMessage(), e);
            throw new ApprovalInternalServerErrorException("승인자 결정 중 오류 발생", e);
        }

        // 4. ApprovalRequest 엔티티 생성
        ApprovalRequest approvalRequest = null;
        try {
            approvalRequest = ApprovalRequest.builder()
                    .requestType(createDto.getRequestType())
                    .applicantId(createDto.getApplicantId())
                    .reason(createDto.getReason())
                    .title(createDto.getTitle())
                    .vacationsId(createDto.getVacationsId())
                    .vacationType(createDto.getVacationType())
                    .certificateId(createDto.getCertificateId())
                    .certificateType(createDto.getCertificateType()) // 클라이언트가 전달한 타입으로 저장
                    .absencesId(createDto.getAbsencesId())
                    .absenceType(createDto.getAbsenceType())
                    .urgency(createDto.getUrgency())
                    .startDate(createDto.getStartDate())
                    .endDate(createDto.getEndDate())
                    .startTime(createDto.getStartTime())
                    .endTime(createDto.getEndTime())
                    .status(ApprovalStatus.PENDING)
                    .requestedAt(LocalDateTime.now())
                    .approverId(approverId)
                    .build();
            log.info("--- 4. 엔티티 생성 완료: {}", approvalRequest);
        } catch (Exception e) {
            log.error("ApprovalRequest 엔티티 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new ApprovalInternalServerErrorException("결재 요청 엔티티 생성 중 오류 발생", e);
        }

        // 5. 저장
        ApprovalRequest savedRequest = null;
        try {
            savedRequest = approvalRepository.save(approvalRequest);
            log.info("--- 5. ApprovalRequest 엔티티 저장 성공. ID: {}", savedRequest.getId());
        } catch (Exception e) {
            log.error("ApprovalRequest 엔티티 저장 중 오류 발생: {}", e.getMessage(), e);
            throw new ApprovalInternalServerErrorException("결재 요청 엔티티 저장 중 오류 발생", e);
        }

        // 6. 응답 생성에 필요한 사용자 정보 조회
        List<Long> userIds = new ArrayList<>(List.of(savedRequest.getApplicantId()));
        if (savedRequest.getApproverId() != null) {
            userIds.add(savedRequest.getApproverId());
        }
        Map<Long, UserResDto> userMap = null;
        try {
            userMap = getUserMap(userIds);
            log.info("--- 6. 사용자 정보 조회 완료. userMap size: {}", userMap.size());
        } catch (Exception e) {
            log.error("사용자 정보 조회 중 오류 발생: {}", e.getMessage(), e);
            throw new ApprovalInternalServerErrorException("사용자 정보 조회 중 오류 발생", e);
        }

        // 7. 최종 응답 DTO 빌드 및 반환
        try {
            return buildResponseDto(savedRequest, userMap);
        } catch (Exception e) {
            log.error("응답 DTO 빌드 중 오류 발생: {}", e.getMessage(), e);
            throw new ApprovalInternalServerErrorException("응답 DTO 빌드 중 오류 발생", e);
        }
    }

    /**
     * 휴가 결재 요청 생성
     */
    @Override
    @Transactional
    public ApprovalRequestResponseDto createVacationApprovalRequest(TokenUserInfo userInfo,
                                                                    VacationApprovalRequestCreateDto createDto) {
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


    /**
     * 부재 결재 요청 생성
     */
    @Override
    @Transactional
    public ApprovalRequestResponseDto createAbsenceApprovalRequest(TokenUserInfo userInfo,
                                                                   AbsenceApprovalRequestCreateDto createDto) {
        String title = "부재 신청";
        if (createDto.getAbsenceType() != null) {
            title += " - " + createDto.getAbsenceType().getDescription();
        }

        ApprovalRequestCreateDto generalDto = ApprovalRequestCreateDto.builder()
                .requestType(RequestType.ABSENCE)
                .applicantId(userInfo.getEmployeeNo())
                .title(title)
                .reason(createDto.getReason())
                .absencesId(createDto.getAbsencesId())
                .absenceType(createDto.getAbsenceType())
                .urgency(createDto.getUrgency())
                .startDate(createDto.getStartDate())
                .endDate(createDto.getEndDate())
                .startTime(createDto.getStartTime())
                .endTime(createDto.getEndTime())
                .build();
        return createApprovalRequest(userInfo, generalDto);
    }

    // ===== 결재 요청 조회 메서드들 =====

    /**
     * 특정 ID로 결재 요청 조회
     */
    @Override
    public ApprovalRequestResponseDto getApprovalRequestById(Long id) {
        ApprovalRequest approvalRequest = approvalRepository.findById(id)
                .orElseThrow(() -> new ApprovalNotFoundException("결재 요청을 찾을 수 없습니다: " + id));

        List<Long> userIds = new ArrayList<>(List.of(approvalRequest.getApplicantId()));
        if (approvalRequest.getApproverId() != null) {
            userIds.add(approvalRequest.getApproverId());
        }
        Map<Long, UserResDto> userMap = getUserMap(userIds);

        return buildResponseDto(approvalRequest, userMap);
    }

    /**
     * 조건에 따라 결재 요청 목록을 동적으로 조회합니다.
     * @param applicantId 신청자 ID
     * @param status 결재 상태 (PENDING, PROCESSED)
     * @param requestType 요청 종류 (VACATION, CERTIFICATE, ABSENCE)
     * @return 조건에 맞는 결재 요청 DTO 목록
     */
    @Override
    public Page<ApprovalRequestResponseDto> getApprovalRequests(Long applicantId, String status, String requestType, Pageable pageable) {
        log.info("getApprovalRequests 호출: applicantId={}, status={}, requestType={}, pageable={}", applicantId, status, requestType, pageable);
        Specification<ApprovalRequest> spec = ApprovalSpecification.withFilter(applicantId, status, requestType);
        Page<ApprovalRequest> requestsPage = approvalRepository.findAll(spec, pageable);

        // 사용자 정보 조회 (신청자와 결재자 모두)
        List<Long> allUserIds = new ArrayList<>();
        requestsPage.forEach(request -> {
            allUserIds.add(request.getApplicantId());
            if (request.getApproverId() != null) {
                allUserIds.add(request.getApproverId());
            }
        });
        Map<Long, UserResDto> userMap = getUserMap(allUserIds.stream().distinct().collect(Collectors.toList()));

        return requestsPage.map(request -> buildResponseDto(request, userMap));
    }

    /**
     * 특정 타입의 결재 요청 조회
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "approvalRequests", key = "#requestType")
    public List<ApprovalRequestResponseDto> getAllApprovalRequests(RequestType requestType) {
        log.info("getAllApprovalRequests 메서드 진입 (requestType 필터링). 요청 유형: {}", requestType);
        List<ApprovalRequest> filteredRequests = approvalRepository.findByRequestType(requestType);
        return buildResponseDtoList(filteredRequests);
    }

    /**
     * 대기 중인 결재 요청 조회 (HR용)
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalRequestResponseDto> getPendingApprovalRequests(TokenUserInfo userInfo, Pageable pageable) {
        if (!"Y".equals(userInfo.getHrRole())) {
            throw new ApprovalForbiddenException("HR 권한이 필요합니다.");
        }

        Page<ApprovalRequest> pendingRequestsPage = approvalRepository.findByStatus(ApprovalStatus.PENDING, pageable);

        // 사용자 정보 조회 (신청자와 결재자 모두)
        List<Long> allUserIds = new ArrayList<>();
        pendingRequestsPage.forEach(request -> {
            allUserIds.add(request.getApplicantId());
            if (request.getApproverId() != null) {
                allUserIds.add(request.getApproverId());
            }
        });
        Map<Long, UserResDto> userMap = getUserMap(allUserIds.stream().distinct().collect(Collectors.toList()));

        return pendingRequestsPage.map(request -> buildResponseDto(request, userMap));
    }

    /**
     * 특정 결재자가 처리한 결재 요청 조회 (HR용)
     */
    @Override
    @Transactional(readOnly = true)
    public List<ApprovalRequestResponseDto> getProcessedApprovalRequestsByApproverId(TokenUserInfo userInfo) {
        if (!"Y".equals(userInfo.getHrRole())) {
            throw new ApprovalForbiddenException("HR 권한이 필요합니다.");
        }

        List<ApprovalRequest> processedRequests = approvalRepository
                .findByApproverIdAndStatusInOrderByProcessedAtDesc(
                        userInfo.getEmployeeNo(),
                        List.of(ApprovalStatus.APPROVED, ApprovalStatus.REJECTED)
                );
        return buildResponseDtoList(processedRequests);
    }

    // ===== 결재 승인/반려 메서드들 =====

    /**
     * 결재 요청 승인
     */
    @Override
    @Transactional
    @CacheEvict(value = "approvalRequests", allEntries = true)
    public ApprovalRequestResponseDto approveApprovalRequest(Long id, Long employeeNo) {
        log.info("approveApprovalRequest 메서드 시작. 요청 ID: {}, 승인자: {}", id, employeeNo);

        ApprovalRequest approvalRequest = getAndValidatePendingRequest(id);
        approvalRequest.setApproverId(employeeNo);
        approvalRequest.approve();

        ApprovalRequest updatedRequest = approvalRepository.save(approvalRequest);
        log.info("결재 요청 승인 처리 완료. 상태: {}", updatedRequest.getStatus());

        // 서비스별 후처리
        processApprovalAftermath(updatedRequest);

        return buildResponseDto(updatedRequest, getUserMapForRequest(updatedRequest));
    }

    /**
     * 결재 요청 반려
     */
    @Override
    @Transactional
    @CacheEvict(value = "approvalRequests", allEntries = true)
    public ApprovalRequestResponseDto rejectApprovalRequest(Long id, TokenUserInfo userInfo,
                                                            ApprovalRejectRequestDto rejectRequestDto) {
        log.info("rejectApprovalRequest 메서드 시작. 요청 ID: {}, 반려자: {}", id, userInfo.getEmployeeNo());

        ApprovalRequest approvalRequest = getAndValidatePendingRequest(id);
        approvalRequest.setApproverId(userInfo.getEmployeeNo());
        approvalRequest.reject(rejectRequestDto.getRejectComment());

        ApprovalRequest updatedRequest = approvalRepository.save(approvalRequest);
        log.info("결재 요청 반려 처리 완료. 상태: {}, 반려 사유: {}",
                updatedRequest.getStatus(), updatedRequest.getRejectComment());

        // 서비스별 후처리
        processRejectionAftermath(updatedRequest);

        return buildResponseDto(updatedRequest, getUserMapForRequest(updatedRequest));
    }

    // ===== 부재 관련 특화 메서드들 =====

    /**
     * 부재 결재 요청 승인
     */
    @Override
    @Transactional
    @CacheEvict(value = "approvalRequests", allEntries = true)
    public ApprovalRequestResponseDto approveAbsenceApprovalRequest(Long id, Long employeeNo) {
        log.info("부재 결재 요청 승인 처리 시작. 요청 ID: {}, 승인자: {}", id, employeeNo);

        ApprovalRequest approvalRequest = getAndValidatePendingRequest(id);

        if (approvalRequest.getRequestType() != RequestType.ABSENCE) {
            throw new ApprovalBadRequestException("부재 결재 요청이 아닙니다.");
        }

        approvalRequest.setApproverId(employeeNo);
        approvalRequest.approve();

        ApprovalRequest updatedRequest = approvalRepository.save(approvalRequest);

        // absence-service에 승인 처리 요청
        if (updatedRequest.getAbsencesId() != null) {
            try {
                absenceServiceClient.approveAbsence(updatedRequest.getAbsencesId(), employeeNo);
                log.info("absence-service에 부재 승인 처리 완료. absenceId: {}", updatedRequest.getAbsencesId());
            } catch (Exception e) {
                log.error("absence-service 부재 승인 처리 실패: {}", e.getMessage(), e);
                throw new ApprovalInternalServerErrorException("부재 승인 처리 중 오류 발생");
            }
        }

        return buildResponseDto(updatedRequest, getUserMapForRequest(updatedRequest));
    }

    /**
     * 부재 결재 요청 반려
     */
    @Override
    @Transactional
    @CacheEvict(value = "approvalRequests", allEntries = true)
    public ApprovalRequestResponseDto rejectAbsenceApprovalRequest(Long id, TokenUserInfo userInfo,
                                                                   ApprovalRejectRequestDto rejectRequestDto) {
        log.info("부재 결재 요청 반려 처리 시작. 요청 ID: {}, 반려자: {}", id, userInfo.getEmployeeNo());

        ApprovalRequest approvalRequest = getAndValidatePendingRequest(id);

        if (approvalRequest.getRequestType() != RequestType.ABSENCE) {
            throw new ApprovalBadRequestException("부재 결재 요청이 아닙니다.");
        }

        approvalRequest.setApproverId(userInfo.getEmployeeNo());
        approvalRequest.reject(rejectRequestDto.getRejectComment());

        ApprovalRequest updatedRequest = approvalRepository.save(approvalRequest);

        // absence-service에 반려 처리 요청
        if (updatedRequest.getAbsencesId() != null) {
            try {
                absenceServiceClient.rejectAbsence(updatedRequest.getAbsencesId(),
                        userInfo.getEmployeeNo(), rejectRequestDto.getRejectComment());
                log.info("absence-service에 부재 반려 처리 완료. absenceId: {}", updatedRequest.getAbsencesId());
            } catch (Exception e) {
                log.error("absence-service 부재 반려 처리 실패: {}", e.getMessage(), e);
                throw new ApprovalInternalServerErrorException("부재 반려 처리 중 오류 발생");
            }
        }

        return buildResponseDto(updatedRequest, getUserMapForRequest(updatedRequest));
    }

    // ===== 부재 조회 메서드들 =====

    /**
     * 부재 결재 통계 조회
     */
    @Override
    @Transactional(readOnly = true)
    public AbsenceApprovalStatisticsDto getAbsenceApprovalStatistics() {
        long totalAbsenceRequests = approvalRepository.countAbsenceApprovalRequests();
        long pendingAbsenceRequests = approvalRepository.countPendingAbsenceApprovalRequests();
        long approvedAbsenceRequests = approvalRepository.countApprovedAbsenceApprovalRequests();
        long rejectedAbsenceRequests = approvalRepository.countRejectedAbsenceApprovalRequests();

        return AbsenceApprovalStatisticsDto.builder()
                .totalAbsenceRequests(totalAbsenceRequests)
                .pendingAbsenceRequests(pendingAbsenceRequests)
                .approvedAbsenceRequests(approvedAbsenceRequests)
                .rejectedAbsenceRequests(rejectedAbsenceRequests)
                .build();
    }

    /**
     * 부재 결재 요청 목록 조회 (페이징)
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalRequestResponseDto> getAbsenceApprovalRequests(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<ApprovalRequest> absenceRequests = approvalRepository
                .findByRequestTypeOrderByRequestedAtDesc(RequestType.ABSENCE);

        return createPagedResponse(absenceRequests, pageable);
    }

    /**
     * 대기 중인 부재 결재 요청 목록 조회 (HR용)
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalRequestResponseDto> getPendingAbsenceApprovalRequests(TokenUserInfo userInfo, int page, int size) {
        if (!"Y".equals(userInfo.getHrRole())) {
            throw new ApprovalForbiddenException("HR 권한이 필요합니다.");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<ApprovalRequest> pendingAbsenceRequests = approvalRepository
                .findByRequestTypeAndStatusOrderByRequestedAtDesc(RequestType.ABSENCE, ApprovalStatus.PENDING, pageable);

        return pendingAbsenceRequests.map(request -> buildResponseDto(request, getUserMapForRequest(request)));
    }

    /**
     * 처리된 부재 결재 요청 목록 조회
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalRequestResponseDto> getProcessedAbsenceApprovalRequests(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ApprovalRequest> processedAbsenceRequests = approvalRepository
                .findProcessedAbsenceApprovalRequests(pageable);

        return processedAbsenceRequests.map(request -> buildResponseDto(request, getUserMapForRequest(request)));
    }

    /**
     * 특정 사용자의 부재 결재 요청 목록 조회
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalRequestResponseDto> getMyAbsenceApprovalRequests(TokenUserInfo userInfo, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<ApprovalRequest> myAbsenceRequests = approvalRepository
                .findByRequestTypeAndApplicantIdOrderByRequestedAtDesc(RequestType.ABSENCE, userInfo.getEmployeeNo());

        return createPagedResponse(myAbsenceRequests, pageable);
    }

    /**
     * 특정 결재자가 처리한 부재 결재 요청 목록 조회
     */
    @Override
    @Transactional(readOnly = true)
    public Page<ApprovalRequestResponseDto> getAbsenceApprovalRequestsProcessedByMe(TokenUserInfo userInfo, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<ApprovalRequest> processedByMeRequests = approvalRepository
                .findByRequestTypeAndApproverIdOrderByProcessedAtDesc(RequestType.ABSENCE, userInfo.getEmployeeNo());

        return createPagedResponse(processedByMeRequests, pageable);
    }

    /**
     * [신규 추가] 부재 결재 요청 수정
     */
    @Override
    @Transactional
    @CacheEvict(value = "approvalRequests", allEntries = true)
    public ApprovalRequestResponseDto updateAbsenceApprovalRequest(Long id, com.playdata.approvalservice.client.dto.AbsenceApprovalRequestUpdateDto updateDto, TokenUserInfo userInfo) {
        log.info("부재 결재 요청 수정 시작. 요청 ID: {}, 사용자: {}", id, userInfo.getEmployeeNo());

        ApprovalRequest approvalRequest = approvalRepository.findById(id)
                .orElseThrow(() -> new ApprovalNotFoundException("결재 요청을 찾을 수 없습니다: " + id));

        // 요청자 본인인지, PENDING 상태인지 검증
        if (!approvalRequest.getApplicantId().equals(userInfo.getEmployeeNo())) {
            throw new ApprovalForbiddenException("본인의 결재 요청만 수정할 수 있습니다.");
        }
        if (approvalRequest.getStatus() != ApprovalStatus.PENDING) {
            throw new ApprovalBadRequestException("대기 중인 결재 요청만 수정할 수 있습니다.");
        }
        if (approvalRequest.getRequestType() != RequestType.ABSENCE) {
            throw new ApprovalBadRequestException("부재 유형의 결재 요청만 수정할 수 있습니다.");
        }

        // 내용 업데이트
        log.info("업데이트 전: reason={}, title={}", approvalRequest.getReason(), approvalRequest.getTitle());
        log.info("수신된 updateDto: reason={}, title={}", updateDto.getReason(), updateDto.getTitle());
        approvalRequest.updateAbsenceRequest(updateDto);

        ApprovalRequest updatedRequest = approvalRepository.save(approvalRequest);
        log.info("부재 결재 요청 수정 완료. 요청 ID: {}, 업데이트 후: reason={}, title={}", updatedRequest.getId(), updatedRequest.getReason(), updatedRequest.getTitle());

        return buildResponseDto(updatedRequest, getUserMapForRequest(updatedRequest));
    }

    /**
     * [신규 추가] 결재 요청 취소(삭제)
     */
    @Override
    @Transactional
    @CacheEvict(value = "approvalRequests", allEntries = true)
    public void cancelApprovalRequest(Long id, TokenUserInfo userInfo) {
        log.info("결재 요청 취소 시작. 요청 ID: {}, 사용자: {}", id, userInfo.getEmployeeNo());

        ApprovalRequest approvalRequest = approvalRepository.findById(id)
                .orElseThrow(() -> new ApprovalNotFoundException("결재 요청을 찾을 수 없습니다: " + id));

        // 요청자 본인인지, PENDING 상태인지 검증
        if (!approvalRequest.getApplicantId().equals(userInfo.getEmployeeNo())) {
            throw new ApprovalForbiddenException("본인의 결재 요청만 취소할 수 있습니다.");
        }
        if (approvalRequest.getStatus() != ApprovalStatus.PENDING) {
            throw new ApprovalBadRequestException("대기 중인 결재 요청만 취소할 수 있습니다.");
        }

        approvalRepository.delete(approvalRequest);
        log.info("결재 요청 취소(삭제) 완료. 요청 ID: {}", id);
    }

    // ===== 휴가 관련 메서드들 =====

    /**
     * 특정 사용자가 특정 날짜에 승인된 휴가가 있는지 확인
     */
    @Override
    public boolean hasApprovedLeave(Long userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59, 999999999);

        List<ApprovalRequest> approvedLeaves = approvalRepository
                .findAllByApplicantIdAndRequestedAtBetweenAndStatus(userId, startOfDay, endOfDay, ApprovalStatus.APPROVED);

        return !approvedLeaves.isEmpty();
    }

    /**
     * 특정 사용자가 특정 날짜에 승인된 휴가의 종류를 조회
     */
    @Override
    public String getApprovedLeaveType(Long userId, LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59, 999999999);

        List<ApprovalRequest> approvedLeaves = approvalRepository
                .findAllByApplicantIdAndRequestedAtBetweenAndStatus(userId, startOfDay, endOfDay, ApprovalStatus.APPROVED);

        return approvedLeaves.stream()
                .filter(request -> request.getRequestType() == RequestType.VACATION)
                .map(ApprovalRequest::getVacationType)
                .findFirst()
                .orElse(null);
    }

    // ===== 프라이빗 헬퍼 메서드들 =====

    /**
     * PENDING 상태의 결재 요청을 조회하고 검증
     */
    private ApprovalRequest getAndValidatePendingRequest(Long id) {
        ApprovalRequest approvalRequest = approvalRepository.findById(id)
                .orElseThrow(() -> new ApprovalNotFoundException("결재 요청을 찾을 수 없습니다: " + id));

        if (approvalRequest.getStatus() != ApprovalStatus.PENDING) {
            throw new ApprovalBadRequestException("결재 요청이 PENDING 상태가 아닙니다.");
        }

        return approvalRequest;
    }

    /**
     * 승인 후 서비스별 후처리
     */
    private void processApprovalAftermath(ApprovalRequest updatedRequest) {
        switch (updatedRequest.getRequestType()) {
            case VACATION -> processVacationApproval(updatedRequest);
            case CERTIFICATE -> processCertificateApproval(updatedRequest);
            case ABSENCE -> processAbsenceApproval(updatedRequest);
        }
    }

    /**
     * 반려 후 서비스별 후처리
     */
    private void processRejectionAftermath(ApprovalRequest updatedRequest) {
        switch (updatedRequest.getRequestType()) {
            case VACATION -> processVacationRejection(updatedRequest);
            case CERTIFICATE -> processCertificateRejection(updatedRequest);
            case ABSENCE -> processAbsenceRejection(updatedRequest);
        }
    }

    /**
     * 휴가 승인 후처리
     */
    private void processVacationApproval(ApprovalRequest request) {
        if (request.getVacationsId() != null) {
            try {
                vacationServiceClient.updateVacationBalanceOnApproval(
                        request.getVacationsId(),
                        ApprovalStatus.APPROVED.name(),
                        request.getApplicantId(),
                        request.getVacationType(),
                        request.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        request.getEndDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        null
                );
                log.info("VacationService 연차 차감 요청 성공.");
            } catch (Exception e) {
                log.error("VacationService 연차 차감 요청 실패: {}", e.getMessage(), e);
                throw new ApprovalInternalServerErrorException("연차 차감 처리 중 오류 발생");
            }
        }
    }

    /**
     * 휴가 반려 후처리
     */
    private void processVacationRejection(ApprovalRequest request) {
        if (request.getVacationsId() != null) {
            try {
                vacationServiceClient.updateVacationBalanceOnApproval(
                        request.getVacationsId(),
                        ApprovalStatus.REJECTED.name(),
                        request.getApplicantId(),
                        request.getVacationType(),
                        request.getStartDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        request.getEndDate().format(DateTimeFormatter.ISO_LOCAL_DATE),
                        request.getRejectComment()
                );
            } catch (Exception e) {
                log.error("VacationService 연차 복구 요청 실패: {}", e.getMessage());
                throw new ApprovalInternalServerErrorException("연차 복구 처리 중 오류 발생");
            }
        }
    }

    /**
     * 증명서 승인 후처리
     */
    private void processCertificateApproval(ApprovalRequest request) {
        if (request.getCertificateId() != null) {
            try {
                Map<Long, UserResDto> userMap = getUserMapForRequest(request);
                String approverName = Optional.ofNullable(request.getApproverId())
                        .map(userMap::get)
                        .map(UserResDto::getUserName).orElse("알 수 없음");

                certificateServiceClient.approveCertificate(request.getCertificateId(),
                        request.getApproverId(), approverName);
                log.info("CertificateService 증명서 승인 요청 성공. certificateId: {}", request.getCertificateId());
            } catch (Exception e) {
                log.error("CertificateService 증명서 승인 요청 실패: {}", e.getMessage(), e);
                throw new ApprovalInternalServerErrorException("증명서 승인 처리 중 오류 발생");
            }
        }
    }

    /**
     * 증명서 반려 후처리
     */
    private void processCertificateRejection(ApprovalRequest request) {
        if (request.getCertificateId() != null) {
            try {
                Map<Long, UserResDto> userMap = getUserMapForRequest(request);
                String approverName = Optional.ofNullable(request.getApproverId())
                        .map(userMap::get)
                        .map(UserResDto::getUserName).orElse("알 수 없음");

                certificateServiceClient.rejectCertificateInternal(request.getCertificateId(), request.getRejectComment(), request.getApproverId(), approverName);
                log.info("CertificateService 증명서 반려 요청 성공. certificateId: {}", request.getCertificateId());
            } catch (Exception e) {
                log.error("CertificateService 증명서 반려 요청 실패: {}", e.getMessage());
                throw new ApprovalInternalServerErrorException("증명서 반려 처리 중 오류 발생");
            }
        }
    }

    /**
     * 부재 승인 후처리
     */
    private void processAbsenceApproval(ApprovalRequest request) {
        if (request.getAbsencesId() != null) {
            log.info("Attempting to call AbsenceService to approve absence. Absence ID: {}, Approver ID: {}", request.getAbsencesId(), request.getApproverId());
            absenceServiceClient.approveAbsence(request.getAbsencesId(), request.getApproverId());
            log.info("Successfully called AbsenceService to approve absence. absencesId: {}", request.getAbsencesId());
        } else {
            log.warn("Absence ID is null. Skipping call to AbsenceService. ApprovalRequest ID: {}", request.getId());
        }
    }

    /**
     * 부재 반려 후처리
     */
    private void processAbsenceRejection(ApprovalRequest request) {
        if (request.getAbsencesId() != null) {
            try {
                absenceServiceClient.rejectAbsence(request.getAbsencesId(),
                        request.getApproverId(), request.getRejectComment());
                log.info("AbsenceService 부재 반려 요청 성공. absencesId: {}", request.getAbsencesId());
            } catch (Exception e) {
                log.error("AbsenceService 부재 반려 요청 실패: {}", e.getMessage());
                throw new ApprovalInternalServerErrorException("부재 반려 처리 중 오류 발생");
            }
        }
    }

    /**
     * 결재 요청에 필요한 사용자 정보 조회
     */
    private Map<Long, UserResDto> getUserMapForRequest(ApprovalRequest request) {
        List<Long> userIds = new ArrayList<>(List.of(request.getApplicantId()));
        if (request.getApproverId() != null) {
            userIds.add(request.getApproverId());
        }
        return getUserMap(userIds);
    }

    /**
     * 결재 요청 리스트를 DTO 리스트로 변환
     */
    private List<ApprovalRequestResponseDto> buildResponseDtoList(List<ApprovalRequest> requests) {
        if (requests.isEmpty()) {
            return List.of();
        }

        List<Long> allUserIds = requests.stream()
                .map(ApprovalRequest::getApplicantId)
                .collect(Collectors.toList());
        requests.stream()
                .filter(req -> req.getApproverId() != null)
                .map(ApprovalRequest::getApproverId)
                .forEach(allUserIds::add);

        allUserIds = allUserIds.stream().distinct().collect(Collectors.toList());
        Map<Long, UserResDto> userMap = getUserMap(allUserIds);

        return requests.stream()
                .map(req -> buildResponseDto(req, userMap))
                .collect(Collectors.toList());
    }

    /**
     * 페이징된 응답 생성
     */
    private Page<ApprovalRequestResponseDto> createPagedResponse(List<ApprovalRequest> requests, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), requests.size());
        List<ApprovalRequest> pagedRequests = requests.subList(start, end);

        List<Long> allUserIds = pagedRequests.stream()
                .map(ApprovalRequest::getApplicantId)
                .collect(Collectors.toList());
        pagedRequests.stream()
                .filter(req -> req.getApproverId() != null)
                .map(ApprovalRequest::getApproverId)
                .forEach(allUserIds::add);
        allUserIds = allUserIds.stream().distinct().collect(Collectors.toList());
        Map<Long, UserResDto> userMap = getUserMap(allUserIds);

        return new PageImpl<>(pagedRequests.stream()
                .map(request -> buildResponseDto(request, userMap))
                .collect(Collectors.toList()), pageable, requests.size());
    }
}