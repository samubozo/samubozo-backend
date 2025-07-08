package com.playdata.vacationservice.vacation.service;

import com.playdata.vacationservice.client.HrServiceClient;
import com.playdata.vacationservice.common.dto.CommonResDto;
import com.playdata.vacationservice.vacation.dto.VacationRequestDto;
import com.playdata.vacationservice.vacation.entity.Vacation;
import com.playdata.vacationservice.vacation.entity.VacationBalance;
import com.playdata.vacationservice.vacation.entity.VacationStatus;
import com.playdata.vacationservice.vacation.repository.VacationBalanceRepository;
import com.playdata.vacationservice.vacation.repository.VacationRepository;
import com.playdata.vacationservice.client.ApprovalServiceClient;
import com.playdata.vacationservice.client.dto.ApprovalRequestDto;
import com.playdata.vacationservice.client.dto.UserDetailDto;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

/**
 * 연차/휴가 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
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
     *
     * @param userId 휴가를 신청하는 사용자의 ID
     * @param requestDto 휴가 신청 정보
     */
    @Transactional
    public void requestVacation(Long userId, VacationRequestDto requestDto) {
        // 1. 사용자의 연차 정보를 조회합니다.
        VacationBalance vacationBalance = vacationBalanceRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("해당 사용자의 연차 정보를 찾을 수 없습니다: " + userId));

        // 2. 신청한 휴가 종류에 따라 차감할 일수를 계산합니다.
        BigDecimal deductionDays = requestDto.getVacationType().getDeductionDays();

        // 3. 남은 연차가 충분한지 확인합니다.
        if (vacationBalance.getRemainingDays().compareTo(deductionDays) < 0) {
            throw new IllegalStateException("남은 연차가 부족합니다.");
        }

        // 4. 휴가 신청 내역을 생성하고 저장합니다. (상태: PENDING_APPROVAL)
        Vacation vacation = Vacation.builder()
                .userId(userId)
                .vacationType(requestDto.getVacationType())
                .vacationStatus(VacationStatus.PENDING_APPROVAL)
                .startDate(requestDto.getStartDate())
                .endDate(requestDto.getEndDate())
                .reason(requestDto.getReason())
                .build();
        vacationRepository.save(vacation);
        log.info("휴가 신청이 데이터베이스에 저장되었습니다. (ID: {})", vacation.getId());

        // 5. HR 서비스에서 사용자 상세 정보를 조회합니다.
        UserDetailDto userDetails;
        try {
            // Feign Client를 통해 HR 서비스에서 사용자 정보를 직접 UserDetailDto로 받아옵니다.
            // 이전 코드에서는 ResponseEntity<CommonResDto>를 받아 복잡하게 변환했지만,
            // 이제는 HrServiceClient에서 직접 UserDetailDto를 반환하므로 코드가 간결해집니다.
            userDetails = hrServiceClient.getMyUserInfo();
            if (userDetails == null) {
                throw new IllegalStateException("HR 서비스로부터 사용자 정보를 가져오는데 실패했습니다. (응답 null)");
            }
        } catch (FeignException e) {
            log.error("HR 서비스 통신 오류: {}", e.getMessage());
            throw new IllegalStateException("HR 서비스 통신 중 오류가 발생했습니다. 다시 시도해주세요.");
        }
        String userName = userDetails.getName();
        String userDepartment = userDetails.getDepartment();

        // 6. 결재 서비스에 결재 생성을 요청합니다.
        String title = String.format("[휴가신청] %s - %s (%s ~ %s)", userName, userDepartment, requestDto.getStartDate(), requestDto.getEndDate());
        String content = String.format("신청자: %s (%s)<br>휴가 종류: %s<br>기간: %s ~ %s<br>사유: %s",
                userName,
                userDepartment,
                requestDto.getVacationType().getDescription(),
                requestDto.getStartDate(),
                requestDto.getEndDate(),
                requestDto.getReason());

        ApprovalRequestDto approvalRequest = ApprovalRequestDto.builder()
                .approvalType("VACATION")
                .userId(userId)
                .title(title)
                .content(content)
                .referenceId(vacation.getId())
                .build();

        try {
            approvalServiceClient.createApproval(approvalRequest);
            log.info("결재 서비스에 휴가(ID: {})에 대한 결재 생성을 요청했습니다.", vacation.getId());
        } catch (FeignException e) {
            log.error("결재 서비스 통신 오류: {}", e.getMessage());
            throw new IllegalStateException("결재 서비스 통신 중 오류가 발생했습니다. 다시 시도해주세요.");
        }

        // 7. 사용한 만큼 연차를 차감합니다.
        vacationBalance.useDays(deductionDays);
        vacationBalanceRepository.save(vacationBalance);
        log.info("사용자(ID: {})의 연차를 {}일 차감했습니다. 남은 연차: {}", userId, deductionDays, vacationBalance.getRemainingDays());
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
        // 사용자 ID로 연차 정보를 조회하거나, 없으면 새로 생성합니다.
        VacationBalance vacationBalance = vacationBalanceRepository.findByUserId(userId)
                .orElse(VacationBalance.builder()
                        .userId(userId)
                        .totalGranted(BigDecimal.ZERO)
                        .usedDays(BigDecimal.ZERO)
                        .build());

        // 지정된 일수만큼 연차를 부여합니다.
        vacationBalance.grantDays(amount);

        // 변경된 연차 정보를 저장합니다.
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
        // 사용자 ID로 연차 정보를 조회하거나, 없으면 새로 생성합니다.
        VacationBalance vacationBalance = vacationBalanceRepository.findByUserId(userId)
                .orElse(VacationBalance.builder()
                        .userId(userId)
                        .totalGranted(java.math.BigDecimal.ZERO)
                        .usedDays(java.math.BigDecimal.ZERO)
                        .build());

        // 연차를 1일 부여합니다.
        vacationBalance.grantDays(java.math.BigDecimal.ONE);

        // 변경된 연차 정보를 저장합니다.
        vacationBalanceRepository.save(vacationBalance);

        log.info("사용자 ID: {} 에게 연차 1일이 부여되었습니다. 총 부여된 연차: {}", userId, vacationBalance.getTotalGranted());
    }
}