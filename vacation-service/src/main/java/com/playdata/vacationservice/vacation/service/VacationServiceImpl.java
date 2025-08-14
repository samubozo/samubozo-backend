package com.playdata.vacationservice.vacation.service;

import com.playdata.vacationservice.client.ApprovalServiceClient;
import com.playdata.vacationservice.client.AttendanceServiceClient;
import com.playdata.vacationservice.client.HrServiceClient;
import com.playdata.vacationservice.client.dto.*;
import com.playdata.vacationservice.common.auth.TokenUserInfo;
import com.playdata.vacationservice.vacation.dto.*;
import com.playdata.vacationservice.vacation.entity.*;
import com.playdata.vacationservice.vacation.repository.VacationBalanceRepository;
import com.playdata.vacationservice.vacation.repository.VacationRepository;
import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 연차/휴가 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 단일 책임 원칙(SRP)을 고려하여 메서드를 분리했습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VacationServiceImpl implements VacationService {

    private final VacationBalanceRepository vacationBalanceRepository;
    private final VacationRepository vacationRepository;
    private final ApprovalServiceClient approvalServiceClient;
    private final HrServiceClient hrServiceClient;
    private final AttendanceServiceClient attendanceServiceClient;

    /**
     * 특정 사용자의 특정 월의 휴가 사용 통계를 조회합니다.
     *
     * @param userId 사용자 ID
     * @param year 연도
     * @param month 월
     * @return 월별 휴가 통계 DTO
     */
    @Override
    public MonthlyVacationStatsDto getMonthlyVacationStats(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Vacation> vacations = vacationRepository.findByUserIdAndStartDateBetweenAndVacationStatus(
                userId, startDate, endDate, VacationStatus.APPROVED
        );

        BigDecimal fullDayVacations = BigDecimal.ZERO;
        BigDecimal halfDayVacations = BigDecimal.ZERO;

        for (Vacation vacation : vacations) {
            if (vacation.getVacationType() == VacationType.ANNUAL_LEAVE) {
                fullDayVacations = fullDayVacations.add(BigDecimal.ONE);
            } else if (vacation.getVacationType() == VacationType.AM_HALF_DAY || vacation.getVacationType() == VacationType.PM_HALF_DAY) {
                halfDayVacations = halfDayVacations.add(BigDecimal.ONE);
            }
        }

        return new MonthlyVacationStatsDto(fullDayVacations, halfDayVacations);
    }

    /**
     * 특정 사용자의 월별 반차 기록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param year   연도
     * @param month  월
     * @return 월별 반차 기록 목록
     */
    @Override
    @Transactional(readOnly = true)
    public List<Vacation> getMonthlyHalfDayVacations(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        return vacationRepository.findByUserIdAndStartDateBetweenAndVacationStatus(
                userId, startDate, endDate, VacationStatus.APPROVED
        ).stream()
                .filter(vacation -> vacation.getVacationType() == VacationType.AM_HALF_DAY || vacation.getVacationType() == VacationType.PM_HALF_DAY)
                .collect(Collectors.toList());
    }


    /**
     * 사용자로부터 휴가 신청을 받아 처리합니다.
     * 이 메서드는 전체 휴가 신청 프로세스를 오케스트레이션합니다.
     *
     * @param userInfo   휴가를 신청하는 사용자의 인증 정보
     * @param requestDto 휴가 신청 정보
     */
    @Override
    @Transactional
    public void requestVacation(TokenUserInfo userInfo, VacationRequestDto requestDto) {
        log.info("requestVacation 메서드 진입. requestDto: {}", requestDto);
        Long employeeNo = userInfo.getEmployeeNo();
        LocalDate startDate = requestDto.getStartDate();
        LocalDate endDate = requestDto.getEndDate();
        String reason = requestDto.getReason();
        

        BigDecimal deductionDays;
        if (requestDto.getVacationType() == VacationType.ANNUAL_LEAVE) {
            // 연차의 경우 시작일과 종료일 사이의 일수를 계산하여 차감
            long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1; // 시작일과 종료일 포함
            deductionDays = new BigDecimal(daysBetween);
        } else {
            // 반차의 경우 VacationType에 정의된 고정된 일수 사용 (0.5일)
            deductionDays = requestDto.getVacationType().getDeductionDays();
        }

        log.info("휴가 신청 요청 - 사용자 ID: {}, 시작일: {}, 종료일: {}, 휴가 종류: {}, 사유: {}",
                employeeNo, startDate, endDate, requestDto.getVacationType(), reason);

        // 1. 휴가 신청 내역 저장
        Vacation savedVacation = saveVacationRequest(employeeNo, requestDto);

        // 2. HR 서비스에서 사용자 상세 정보 조회
        UserDetailDto userDetails = fetchUserDetailsFromHrService();

        // 3. 결재 서비스에 결재 요청 및 approvalRequestId 저장
        ApprovalRequestResponseDto approvalResponse = requestApproval(savedVacation.getId(), userDetails, userInfo, requestDto.getVacationType(), startDate, endDate, reason);
        savedVacation.setApprovalRequestId(approvalResponse.getId());
        vacationRepository.save(savedVacation);

        log.info("사용자(ID: {})의 휴가 신청 프로세스가 완료되었습니다. 신청 ID: {}", employeeNo, savedVacation.getId());
    }

    /**
     * 특정 사용자에게 지정된 일수만큼의 연차를 부여합니다.
     * 만약 해당 사용자의 연차 정보가 존재하지 않으면 새로 생성하여 부여합니다.
     *
     * @param userId 연차를 부여할 사용자의 ID
     * @param days 부여할 연차 일수
     */
    @Override
    @Transactional
    public void grantAnnualLeave(Long userId, int days) {
        BigDecimal amount = new BigDecimal(days);
        VacationBalance vacationBalance = vacationBalanceRepository.findByUserId(userId)
                .orElse(VacationBalance.builder()
                        .userId(userId)
                        .totalGranted(BigDecimal.ZERO)
                        .usedDays(BigDecimal.ZERO)
                        .build());

        vacationBalance.grantDays(amount);
        vacationBalanceRepository.save(vacationBalance);

        log.info("사용자 ID: {} 에게 연차 {}일이 부여되었습니다. 총 부여된 연차: {}", userId, days, vacationBalance.getTotalGranted());
    }

    /**
     * 특정 사용자에게 월별 정기 연차를 1일 부여합니다.
     * 만약 해당 사용자의 연차 정보가 존재하지 않으면 새로 생성하여 1일을 부여합니다.
     *
     * @param userId 연차를 부여할 사용자의 ID
     */
    @Override
    @Transactional
    public void grantMonthlyLeave(Long userId) {
        VacationBalance vacationBalance = vacationBalanceRepository.findByUserId(userId)
                .orElse(VacationBalance.builder()
                        .userId(userId)
                        .totalGranted(java.math.BigDecimal.ZERO)
                        .usedDays(java.math.BigDecimal.ZERO)
                        .build());

        vacationBalance.grantDays(java.math.BigDecimal.ONE);
        vacationBalanceRepository.save(vacationBalance);

        log.info("사용자 ID: {} 에게 연차 1일이 부여되었습니다. 총 부여된 연차: {}", userId, vacationBalance.getTotalGranted());
    }

    /**
     * 특정 사용자의 연차 현황(총 연차, 사용 연차, 남은 연차)을 조회합니다.
     *
     * @param userId 연차 현황을 조회할 사용자의 ID
     * @return 연차 현황 정보를 담은 VacationBalanceResDto
     * @throws IllegalArgumentException 해당 사용자의 연차 정보를 찾을 수 없는 경우
     */
    @Override
    @Transactional(readOnly = true)
    public VacationBalanceResDto getVacationBalance(Long userId) {
        VacationBalance vacationBalance = vacationBalanceRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("해당 사용자의 연차 정보를 찾을 수 없습니다. (User ID: " + userId + ")"));

        return VacationBalanceResDto.from(vacationBalance);
    }

    /**
     * 현재 로그인된 사용자의 모든 휴가 신청 내역을 조회합니다.
     *
     * @param userId 현재 로그인된 사용자의 ID
     * @return 휴가 신청 내역 DTO 목록
     */
    @Override
    public Page<VacationHistoryResDto> getMyVacationRequests(Long userId, Pageable pageable) {
        Page<Vacation> vacationsPage = vacationRepository.findByUserId(userId, pageable);

        // 현재 로그인된 사용자의 부서 정보 조회 (한 번만 호출)
        String applicantDepartment = null;
        try {
            UserFeignResDto currentUserInfo = hrServiceClient.getMyUserInfo();
            if (currentUserInfo != null && currentUserInfo.getDepartment() != null) {
                applicantDepartment = currentUserInfo.getDepartment().getName();
            }
        } catch (FeignException e) {
            log.error("HR 서비스 통신 오류 (getMyUserInfo) for current user: {}", e.getMessage());
            // 오류 발생 시 부서 정보 없이 진행
        }

        final String finalApplicantDepartment = applicantDepartment; // 람다 표현식에서 사용하기 위해 final 변수로 선언

        return vacationsPage.map(vacation -> {
            String approverName = null;
            Long approverEmployeeNo = null;
            java.time.LocalDate processedAt = null;

            if (vacation.getApprovalRequestId() != null) {
                try {
                    ApprovalRequestResponseDto approvalResponse = approvalServiceClient.getApprovalRequestById(vacation.getApprovalRequestId());
                    approverName = approvalResponse.getApproverName();
                    approverEmployeeNo = approvalResponse.getApproverId();
                    processedAt = approvalResponse.getProcessedAt();
                } catch (FeignException e) {
                    log.error("결재 서비스 통신 오류 (getApprovalRequestById) for approvalRequestId {}: {}", vacation.getApprovalRequestId(), e.getMessage());
                    // 오류 발생 시 해당 휴가 신청은 결재자 정보 없이 반환
                }
            }
            return VacationHistoryResDto.from(vacation, approverName, approverEmployeeNo, processedAt, finalApplicantDepartment);
        });
    }

    /**
     * 결재 대기 중인 모든 휴가 신청 목록을 조회합니다.
     * 각 신청에 대한 신청자 정보를 HR 서비스로부터 가져와 함께 반환합니다.
     *
     * @return 결재 대기 중인 휴가 신청 DTO 목록
     */
    @Override
    @Transactional(readOnly = true)
    public List<PendingApprovalDto> getPendingApprovals() {
        List<Vacation> pendingVacations = vacationRepository.findByVacationStatus(VacationStatus.PENDING_APPROVAL);

        if (pendingVacations.isEmpty()) {
            return Collections.emptyList();
        }

        // 신청자 ID 목록 추출
        List<Long> userIds = pendingVacations.stream()
                .map(Vacation::getUserId)
                .distinct()
                .collect(Collectors.toList());

        // HR 서비스에서 사용자 정보 조회
        List<UserResDto> usersInfo;
        try {
            usersInfo = hrServiceClient.getUsersInfo(userIds);
            if (usersInfo == null || usersInfo.isEmpty()) {
                log.warn("HR 서비스로부터 사용자 정보를 가져오지 못했습니다. userIds: {}", userIds);
                return Collections.emptyList();
            }
        } catch (FeignException e) {
            log.error("HR 서비스 통신 오류 (getUsersInfo): {}", e.getMessage());
            throw new IllegalStateException("HR 서비스 통신 중 오류가 발생했습니다. 다시 시도해주세요.");
        }

        // 사용자 ID를 키로 하는 맵 생성 (빠른 조회를 위해)
        Map<Long, UserResDto> userMap = usersInfo.stream()
                .collect(Collectors.toMap(UserResDto::getEmployeeNo, Function.identity()));

        // PendingApprovalDto로 변환
        return pendingVacations.stream()
                .map(vacation -> {
                    UserResDto user = userMap.get(vacation.getUserId());
                    if (user == null) {
                        log.warn("사용자 ID {}에 대한 HR 정보가 없습니다. 해당 휴가 신청은 건너뜁니다.", vacation.getUserId());
                        return null; // 또는 예외 처리
                    }
                    return PendingApprovalDto.from(vacation, user);
                })
                .filter(Objects::nonNull) // null이 아닌 항목만 필터링
                .collect(Collectors.toList());
    }

    /**
     * 휴가 신청을 승인 처리합니다.
     *
     * @param vacationId 승인할 휴가 신청 ID
     */
    @Override
    @Transactional
    public void approveVacation(Long vacationId) {
        Vacation vacation = vacationRepository.findById(vacationId)
                .orElseThrow(() -> new EntityNotFoundException("해당 휴가 신청을 찾을 수 없습니다: " + vacationId));

        if (vacation.getVacationStatus() != VacationStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("대기 중인 휴가 신청만 승인할 수 있습니다.");
        }

        // 연차 차감 로직은 approval-service의 콜백을 통해 updateVacationBalanceOnApproval에서 처리됩니다.
        // 휴가 상태 변경 로직도 updateVacationBalanceOnApproval에서 처리됩니다.

        // 결재 서비스에 상태 업데이트 요청
        try {
            approvalServiceClient.approveApprovalRequest(vacation.getApprovalRequestId(), null); // userInfo는 Feign Interceptor로 전달
            log.info("휴가 신청 (ID: {})이 승인되었습니다. 결재 서비스에 상태 업데이트 요청 완료.", vacationId);
        } catch (FeignException e) {
            log.error("결재 서비스 통신 오류 (approveVacation): {}", e.getMessage());
            throw new IllegalStateException("결재 서비스 통신 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
    }

    /**
     * 휴가 신청을 반려 처리합니다.
     *
     * @param vacationId 반려할 휴가 신청 ID
     * @param requestDto 반려 사유를 포함하는 DTO
     */
    @Override
    @Transactional
    public void rejectVacation(Long vacationId, ApprovalRequestProcessDto requestDto) {
        Vacation vacation = vacationRepository.findById(vacationId)
                .orElseThrow(() -> new EntityNotFoundException("해당 휴가 신청을 찾을 수 없습니다: " + vacationId));

        if (vacation.getVacationStatus() != VacationStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("대기 중인 휴가 신청만 반려할 수 있습니다.");
        }

        // 연차 복구 로직은 approval-service의 콜백을 통해 updateVacationBalanceOnApproval에서 처리됩니다.
        // 휴가 상태 변경 로직도 updateVacationBalanceOnApproval에서 처리됩니다.

        // 결재 서비스에 상태 업데이트 요청
        try {
            approvalServiceClient.rejectApprovalRequest(vacation.getApprovalRequestId(), requestDto); // requestDto를 직접 전달
            log.info("휴가 신청 (ID: {})이 반려되었습니다. 결재 서비스에 상태 업데이트 요청 완료. 사유: {}", vacationId, requestDto.getRejectComment());
        } catch (FeignException e) {
            log.error("결재 서비스 통신 오류 (rejectVacation): {}", e.getMessage());
            throw new IllegalStateException("결재 서비스 통신 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
    }

    /**
     * 휴가 신청을 수정합니다. (PENDING_APPROVAL 상태만 가능)
     *
     * @param vacationId 수정할 휴가 신청 ID
     * @param userId 요청한 사용자의 ID
     * @param requestDto 수정할 휴가 정보를 담은 DTO
     */
    @Override
    @Transactional
    public void updateVacationRequest(Long vacationId, Long userId, VacationRequestDto requestDto) {
        Vacation vacation = vacationRepository.findById(vacationId)
                .orElseThrow(() -> new EntityNotFoundException("해당 휴가 신청을 찾을 수 없습니다: " + vacationId));

        // 신청자 본인만 수정 가능하도록 검증
        if (!vacation.getUserId().equals(userId)) {
            throw new IllegalStateException("본인의 휴가 신청만 수정할 수 있습니다.");
        }

        // PENDING_APPROVAL 상태의 휴가만 수정 가능
        if (vacation.getVacationStatus() != VacationStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("대기 중인 휴가 신청만 수정할 수 있습니다.");
        }

        // 휴가 정보 업데이트
        vacation.setStartDate(requestDto.getStartDate());
        vacation.setEndDate(requestDto.getEndDate());
        vacation.setVacationType(requestDto.getVacationType());
        vacation.setReason(requestDto.getReason());

        vacationRepository.save(vacation);
        log.info("휴가 신청 (ID: {})이 수정되었습니다.", vacationId);

        // TODO: approval-service의 해당 결재 요청도 업데이트해야 할 수 있습니다.
        // 현재 approval-service에 결재 요청 수정 API가 없으므로, 필요 시 추가 구현
    }

    // --- Private Helper Methods for SRP ---

    /**
     * 연차 잔액을 확인하고 차감합니다.
     * @param employeeNo 사용자 사번
     * @param deductionDays 차감할 일수
     */
    @Override
    public void deductVacationBalance(Long employeeNo, BigDecimal deductionDays) {
        VacationBalance vacationBalance = vacationBalanceRepository.findByUserId(employeeNo)
                .orElseThrow(() -> new EntityNotFoundException("해당 사용자의 연차 정보를 찾을 수 없습니다: " + employeeNo));

        if (vacationBalance.getRemainingDays().compareTo(deductionDays) < 0) {
            throw new IllegalStateException("남은 연차가 부족합니다.");
        }
        vacationBalance.useDays(deductionDays);
        vacationBalanceRepository.save(vacationBalance);
        log.info("사용자(ID: {})의 연차를 {}일 차감했습니다. 남은 연차: {}", employeeNo, deductionDays, vacationBalance.getRemainingDays());
    }

    /**
     * 연차 잔액을 복구합니다.
     * @param employeeNo 사용자 사번
     * @param restoredDays 복구할 일수
     */
    @Override
    public void restoreVacationBalance(Long employeeNo, BigDecimal restoredDays) {
        VacationBalance vacationBalance = vacationBalanceRepository.findByUserId(employeeNo)
                .orElseThrow(() -> new EntityNotFoundException("해당 사용자의 연차 정보를 찾을 수 없습니다: " + employeeNo));

        // 사용된 연차를 되돌리는 것이므로 usedDays를 감소시킵니다.
        vacationBalance.useDays(restoredDays.negate()); // useDays 메서드에서 음수 처리 가능하도록 수정 필요
        vacationBalanceRepository.save(vacationBalance);
        log.info("사용자(ID: {})의 연차를 {}일 복구했습니다. 남은 연차: {}", employeeNo, restoredDays, vacationBalance.getRemainingDays());
    }

    /**
     * 휴가 신청 내역을 데이터베이스에 저장합니다.
     * @param employeeNo 사용자 사번
     * @param requestDto 휴가 신청 DTO
     * @return 저장된 휴가 엔티티
     */
    private Vacation saveVacationRequest(Long employeeNo, VacationRequestDto requestDto) {
        Vacation vacation = Vacation.builder()
                .userId(employeeNo)
                .vacationType(requestDto.getVacationType())
                .vacationStatus(VacationStatus.PENDING_APPROVAL)
                .startDate(requestDto.getStartDate())
                .endDate(requestDto.getEndDate())
                .reason(requestDto.getReason())
                .build();
        return vacationRepository.save(vacation);
    }

    /**
     * 특정 결재자가 처리한 (승인 또는 반려) 휴가 신청 내역을 조회합니다.
     *
     * @param userInfo 현재 로그인된 사용자의 인증 정보 (결재자 ID 추출용)
     * @return 처리된 휴가 신청 내역 DTO 목록
     */
    @Override
    public List<VacationHistoryResDto> getProcessedVacationApprovals(TokenUserInfo userInfo) {
        // approval-service에서 해당 결재자가 처리한 모든 결재 요청을 조회
        List<ApprovalRequestResponseDto> processedApprovals;
        try {
            processedApprovals = approvalServiceClient.getProcessedApprovalRequestsByApproverId(userInfo);
        } catch (FeignException e) {
            log.error("결재 서비스 통신 오류 (getProcessedApprovalRequestsByApproverId): {}", e.getMessage());
            throw new IllegalStateException("결재 서비스 통신 중 오류가 발생했습니다. 다시 시도해주세요.");
        }

        if (processedApprovals.isEmpty()) {
            return Collections.emptyList();
        }

        // 휴가 신청에 해당하는 approvalRequestId만 필터링하고, 해당 vacationsId를 추출
        List<Long> vacationIds = processedApprovals.stream()
                .filter(approval -> approval.getRequestType() == RequestType.VACATION && approval.getVacationsId() != null)
                .map(ApprovalRequestResponseDto::getVacationsId)
                .collect(Collectors.toList());

        if (vacationIds.isEmpty()) {
            return Collections.emptyList();
        }

        // vacation-service의 DB에서 해당 휴가 엔티티들을 조회
        Map<Long, Vacation> vacationMap = vacationRepository.findAllById(vacationIds).stream()
                .collect(Collectors.toMap(Vacation::getId, Function.identity()));

        // 모든 신청자 ID 추출
        List<Long> applicantIds = processedApprovals.stream()
                .map(ApprovalRequestResponseDto::getApplicantId)
                .distinct()
                .collect(Collectors.toList());

        // HR 서비스에서 신청자 정보 조회
        Map<Long, UserResDto> applicantInfoMap;
        try {
            List<UserResDto> usersInfo = hrServiceClient.getUsersInfo(applicantIds);
            applicantInfoMap = usersInfo.stream()
                    .collect(Collectors.toMap(UserResDto::getEmployeeNo, Function.identity()));
        } catch (FeignException e) {
            log.error("HR 서비스 통신 오류 (getUsersInfo) for applicants: {}", e.getMessage());
            throw new IllegalStateException("HR 서비스 통신 중 오류가 발생했습니다. 다시 시도해주세요.");
        }

        // ApprovalRequestResponseDto와 Vacation 엔티티를 결합하여 VacationHistoryResDto 생성
        return processedApprovals.stream()
                .filter(approval -> approval.getRequestType() == RequestType.VACATION && vacationMap.containsKey(approval.getVacationsId()))
                .map(approval -> {
                    Vacation vacation = vacationMap.get(approval.getVacationsId());
                    UserResDto applicantInfo = applicantInfoMap.get(approval.getApplicantId());
                    String applicantDepartment = (applicantInfo != null && applicantInfo.getDepartment() != null) ? applicantInfo.getDepartment().getName() : null;

                    return VacationHistoryResDto.from(
                            vacation,
                            approval.getApproverName(),
                            approval.getApproverId(),
                            approval.getProcessedAt(),
                            applicantDepartment
                    );
                })
                .collect(Collectors.toList());
    }

    /**
     * 휴가 엔티티의 상태와 반려 사유를 업데이트합니다.
     *
     * @param vacationId 휴가 ID
     * @param status 업데이트할 휴가 상태
     * @param rejectComment 반려 사유 (반려 시에만 사용)
     */
    @Override
    @Transactional
    public void updateVacationStatus(Long vacationId, VacationStatus status, String rejectComment) {
        Vacation vacation = vacationRepository.findById(vacationId)
                .orElseThrow(() -> new EntityNotFoundException("해당 휴가 신청을 찾을 수 없습니다: " + vacationId));
        vacation.setVacationStatus(status);
        if (status == VacationStatus.REJECTED) {
            vacation.setRejectComment(rejectComment);
        }
        vacationRepository.save(vacation);
        log.info("휴가 (ID: {})의 상태가 {}로 업데이트되었습니다. 반려 사유: {}", vacationId, status, rejectComment);
    }

    /**
     * HR 서비스에서 사용자 상세 정보를 조회합니다.
     * @return 사용자 상세 정보 DTO
     */
    private UserDetailDto fetchUserDetailsFromHrService() {
        UserFeignResDto userFeignResDto;
        try {
            userFeignResDto = hrServiceClient.getMyUserInfo();
            if (userFeignResDto == null) {
                throw new IllegalStateException("HR 서비스로부터 사용자 정보를 가져오는데 실패했습니다. (응답 null)");
            }
        } catch (FeignException e) {
            log.error("HR 서비스 통신 오류: {}", e.getMessage());
            throw new IllegalStateException("HR 서비스 통신 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
        // UserFeignResDto를 UserDetailDto로 변환
        return new UserDetailDto(
                userFeignResDto.getEmployeeNo(),
                userFeignResDto.getUserName(),
                userFeignResDto.getDepartment() != null ? userFeignResDto.getDepartment().getName() : null,
                userFeignResDto.getHrRole() // hrRole 추가
        );
    }

    /**
     * 결재 서비스에 결재 생성을 요청합니다.
     * @param vacationId 휴가 신청 ID
     * @param userDetails 사용자 상세 정보
     * @param vacationType 휴가 종류
     * @param startDate 휴가 시작일
     * @param endDate 휴가 종료일
     * @param reason 휴가 사유
     */
    private ApprovalRequestResponseDto requestApproval(Long vacationId, UserDetailDto userDetails, TokenUserInfo userInfo,
                                 VacationType vacationType, LocalDate startDate, LocalDate endDate, String reason) {

        String title = String.format("%s 휴가 신청 (%s ~ %s)", vacationType.getDescription(), startDate, endDate);

        VacationApprovalRequestCreateDto approvalRequest = VacationApprovalRequestCreateDto.builder()
                .requestType(RequestType.VACATION.name())
                .applicantId(userInfo.getEmployeeNo())
                .reason(reason)
                .vacationsId(vacationId)
                .vacationType(vacationType.name())
                .title(title)
                .startDate(startDate)
                .endDate(endDate)
                .build();

        try {
            return approvalServiceClient.createApproval(approvalRequest);
        } catch (FeignException e) {
            log.error("결재 서비스 통신 오류: [{}]{}", e.status(), e.getMessage());
            // FeignException의 상태 코드를 기반으로 ResponseStatusException을 던져 프론트엔드에 정확한 오류 전달
            throw new ResponseStatusException(HttpStatus.valueOf(e.status()), e.getMessage());
        }
    }

    /**
     * 여러 사용자의 특정 기간 동안 승인된 유급 휴가 일수를 계산합니다.
     *
     * @param userIds   사용자 ID 목록
     * @param startDate 시작일
     * @param endDate   종료일
     * @return 사용자 ID를 key로, 총 휴가 일수를 value로 갖는 Map
     */
    @Override
    public Map<Long, Double> getApprovedPaidVacationDaysForUsers(List<Long> userIds, LocalDate startDate, LocalDate endDate) {
        List<Vacation> approvedVacations = vacationRepository.findByUserIdInAndStartDateBetweenAndVacationStatus(
                userIds, startDate, endDate, VacationStatus.APPROVED
        );

        return approvedVacations.stream()
                .filter(v -> v.getVacationType().isDeducted()) // 연차 차감 대상 휴가만 필터링
                .collect(Collectors.groupingBy(
                        Vacation::getUserId,
                        Collectors.summingDouble(v -> {
                            if (v.getVacationType() == VacationType.ANNUAL_LEAVE) {
                                return (double) (ChronoUnit.DAYS.between(v.getStartDate(), v.getEndDate()) + 1);
                            } else {
                                return v.getVacationType().getDeductionDays().doubleValue();
                            }
                        })
                ));
    }

    @Override
    public void createWorkStatusForApprovedVacation(Long vacationId, VacationType vacationType) {
        log.info("Requesting to create WorkStatus for approved vacation. vacationId: {}", vacationId);

        // 1. vacationId로 휴가 정보 조회
        Vacation vacation = vacationRepository.findById(vacationId)
                .orElseThrow(() -> new EntityNotFoundException("해당 휴가 신청을 찾을 수 없습니다: " + vacationId));

        // 2. DTO 생성
        WorkStatusCreateRequestDto requestDto = WorkStatusCreateRequestDto.builder()
                .userId(vacation.getUserId())
                .startDate(vacation.getStartDate())
                .endDate(vacation.getEndDate())
                .vacationType(vacationType.name())
                .reason(vacation.getReason())
                .build();

        // 3. Feign Client 호출
        try {
            attendanceServiceClient.createWorkStatusForVacation(requestDto);
            log.info("Successfully requested to create WorkStatus for vacationId: {}", vacationId);
        } catch (FeignException e) {
            log.error("Failed to request WorkStatus creation for vacationId: {}. status: {}, message: {}",
                    vacationId, e.status(), e.getMessage());
            // 지금은 로그만 남기고 넘어갑니다.
        }
    }
}