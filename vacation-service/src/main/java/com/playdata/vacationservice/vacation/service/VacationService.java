package com.playdata.vacationservice.vacation.service;

import com.playdata.vacationservice.client.HrServiceClient;
import com.playdata.vacationservice.common.auth.TokenUserInfo;
import com.playdata.vacationservice.vacation.dto.VacationRequestDto;
import com.playdata.vacationservice.vacation.entity.*;
import com.playdata.vacationservice.vacation.repository.VacationBalanceRepository;
import com.playdata.vacationservice.vacation.repository.VacationRepository;
import com.playdata.vacationservice.client.ApprovalServiceClient;
import com.playdata.vacationservice.client.dto.ApprovalRequestDto;
import com.playdata.vacationservice.client.dto.UserDetailDto;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate; // LocalDate 임포트 추가

/**
 * 연차/휴가 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 단일 책임 원칙(SRP)을 고려하여 메서드를 분리했습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VacationService {

    private final VacationBalanceRepository vacationBalanceRepository;
    private final VacationRepository vacationRepository;
    private final ApprovalServiceClient approvalServiceClient;
    private final HrServiceClient hrServiceClient;

    /**
     * 사용자로부터 휴가 신청을 받아 처리합니다.
     * 이 메서드는 전체 휴가 신청 프로세스를 오케스트레이션합니다.
     *
     * @param userInfo   휴가를 신청하는 사용자의 인증 정보
     * @param requestDto 휴가 신청 정보
     */
    @Transactional
    public void requestVacation(TokenUserInfo userInfo, VacationRequestDto requestDto) {
        Long employeeNo = userInfo.getEmployeeNo();
        BigDecimal deductionDays = requestDto.getVacationType().getDeductionDays();
        LocalDate startDate = requestDto.getStartDate();
        LocalDate endDate = requestDto.getEndDate();
        String reason = requestDto.getReason();
        String vacationTypeDescription = requestDto.getVacationType().getDescription();

        // 1. 연차 잔액 확인 및 차감
        deductVacationBalance(employeeNo, deductionDays);

        // 2. 휴가 신청 내역 저장
        Vacation savedVacation = saveVacationRequest(employeeNo, requestDto);

        // 3. HR 서비스에서 사용자 상세 정보 조회
        UserDetailDto userDetails = fetchUserDetailsFromHrService();

        // 4. 결재 서비스에 결재 요청
        requestApproval(employeeNo, savedVacation.getId(), userDetails, vacationTypeDescription, startDate, endDate, reason);

        log.info("사용자(ID: {})의 휴가 신청 프로세스가 완료되었습니다. 신청 ID: {}", employeeNo, savedVacation.getId());
    }

    /**
     * 특정 사용자에게 지정된 일수만큼의 연차를 부여합니다.
     * 만약 해당 사용자의 연차 정보가 존재하지 않으면 새로 생성하여 부여합니다.
     *
     * @param userId 연차를 부여할 사용자의 ID
     * @param days 부여할 연차 일수
     */
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

    // --- Private Helper Methods for SRP ---

    /**
     * 연차 잔액을 확인하고 차감합니다.
     * @param employeeNo 사용자 사번
     * @param deductionDays 차감할 일수
     */
    private void deductVacationBalance(Long employeeNo, BigDecimal deductionDays) {
        VacationBalance vacationBalance = vacationBalanceRepository.findByUserId(employeeNo)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자의 연차 정보를 찾을 수 없습니다: " + employeeNo));

        if (vacationBalance.getRemainingDays().compareTo(deductionDays) < 0) {
            throw new IllegalStateException("남은 연차가 부족합니다.");
        }
        vacationBalance.useDays(deductionDays);
        vacationBalanceRepository.save(vacationBalance);
        log.info("사용자(ID: {})의 연차를 {}일 차감했습니다. 남은 연차: {}", employeeNo, deductionDays, vacationBalance.getRemainingDays());
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
     * HR 서비스에서 사용자 상세 정보를 조회합니다.
     * @return 사용자 상세 정보 DTO
     */
    private UserDetailDto fetchUserDetailsFromHrService() {
        UserDetailDto userDetails;
        try {
            userDetails = hrServiceClient.getMyUserInfo();
            if (userDetails == null) {
                throw new IllegalStateException("HR 서비스로부터 사용자 정보를 가져오는데 실패했습니다. (응답 null)");
            }
        } catch (FeignException e) {
            log.error("HR 서비스 통신 오류: {}", e.getMessage());
            throw new IllegalStateException("HR 서비스 통신 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
        return userDetails;
    }

    /**
     * 결재 서비스에 결재 생성을 요청합니다.
     * @param employeeNo 사용자 사번
     * @param vacationId 휴가 신청 ID
     * @param userDetails 사용자 상세 정보
     * @param vacationTypeDescription 휴가 종류 설명
     * @param startDate 휴가 시작일
     * @param endDate 휴가 종료일
     * @param reason 휴가 사유
     */
    private void requestApproval(Long employeeNo, Long vacationId, UserDetailDto userDetails,
                                 String vacationTypeDescription, LocalDate startDate, LocalDate endDate, String reason) {

        ApprovalRequestDto approvalRequest = ApprovalRequestDto.builder()
                .requestType(RequestType.VACATION)
                .applicantId(employeeNo)
                .reason(reason)
                .vacationsId(vacationId)
                .build();

        try {
            approvalServiceClient.createApproval(approvalRequest);
            log.info("결재 서비스에 휴가(ID: {})에 대한 결재 생성을 요청했습니다.", vacationId);
        } catch (FeignException e) {
            log.error("결재 서비스 통신 오류: {}", e.getMessage());
            throw new IllegalStateException("결재 서비스 통신 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
    }
}
