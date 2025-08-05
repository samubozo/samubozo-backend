package com.playdata.vacationservice.vacation.service;

import com.playdata.vacationservice.common.auth.TokenUserInfo;
import com.playdata.vacationservice.vacation.dto.ApprovalRequestProcessDto;
import com.playdata.vacationservice.vacation.dto.MonthlyVacationStatsDto;
import com.playdata.vacationservice.vacation.dto.PendingApprovalDto;
import com.playdata.vacationservice.vacation.dto.VacationBalanceResDto;
import com.playdata.vacationservice.vacation.dto.VacationHistoryResDto;
import com.playdata.vacationservice.vacation.dto.VacationRequestDto;
import com.playdata.vacationservice.vacation.entity.Vacation;
import com.playdata.vacationservice.vacation.entity.VacationStatus;

import java.math.BigDecimal; // BigDecimal import 추가
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface VacationService {

    MonthlyVacationStatsDto getMonthlyVacationStats(Long userId, int year, int month);

    List<Vacation> getMonthlyHalfDayVacations(Long userId, int year, int month);

    void requestVacation(TokenUserInfo userInfo, VacationRequestDto requestDto);

    void grantAnnualLeave(Long userId, int days);

    void grantMonthlyLeave(Long userId);

    VacationBalanceResDto getVacationBalance(Long userId);

    org.springframework.data.domain.Page<VacationHistoryResDto> getMyVacationRequests(Long userId, org.springframework.data.domain.Pageable pageable);

    List<PendingApprovalDto> getPendingApprovals();

    void approveVacation(Long vacationId);

    void rejectVacation(Long vacationId, ApprovalRequestProcessDto requestDto);

    void updateVacationRequest(Long vacationId, Long userId, VacationRequestDto requestDto);

    List<VacationHistoryResDto> getProcessedVacationApprovals(TokenUserInfo userInfo);

    void updateVacationStatus(Long vacationId, VacationStatus status, String rejectComment);

    Map<Long, Double> getApprovedPaidVacationDaysForUsers(List<Long> userIds, LocalDate startDate, LocalDate endDate);

    // 추가된 메서드
    void deductVacationBalance(Long employeeNo, BigDecimal deductionDays);

    // 추가된 메서드
    void restoreVacationBalance(Long employeeNo, BigDecimal restoredDays);
}