package com.playdata.vacationservice.vacation.service;

import com.playdata.vacationservice.vacation.dto.VacationRequestDto;
import com.playdata.vacationservice.vacation.dto.VacationBalanceResDto; // 추가
import com.playdata.vacationservice.vacation.entity.Vacation;
import com.playdata.vacationservice.vacation.entity.VacationBalance;
import com.playdata.vacationservice.vacation.entity.VacationType;
import com.playdata.vacationservice.vacation.repository.VacationBalanceRepository;
import com.playdata.vacationservice.vacation.repository.VacationRepository;
import com.playdata.vacationservice.client.ApprovalServiceClient;
import com.playdata.vacationservice.client.HrServiceClient;
import com.playdata.vacationservice.client.dto.ApprovalRequestDto;
import com.playdata.vacationservice.client.dto.UserDetailDto;
import com.playdata.vacationservice.common.auth.TokenUserInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VacationServiceTest {

    // --- mocks for dependencies ---
    private VacationBalanceRepository vacationBalanceRepository;
    private VacationRepository        vacationRepository;
    private ApprovalServiceClient     approvalServiceClient;
    private HrServiceClient           hrServiceClient;

    // --- system under test ---
    private VacationService vacationService;

    // --- shared test data ---
    private Long          userId;
    private TokenUserInfo userInfo;
    private VacationRequestDto requestDto;
    private VacationBalance    vacationBalance;
    private UserDetailDto      userDetailDto;

    @BeforeEach
    void setUp() {
        // 1) Mockito mocks 생성
        vacationBalanceRepository = mock(VacationBalanceRepository.class);
        vacationRepository        = mock(VacationRepository.class);
        approvalServiceClient     = mock(ApprovalServiceClient.class);
        hrServiceClient           = mock(HrServiceClient.class);

        // 2) 테스트용 기본 데이터 설정
        userId = 1L;
        userInfo = new TokenUserInfo("test@example.com", "USER", userId);
        requestDto = new VacationRequestDto(
                VacationType.ANNUAL_LEAVE,
                LocalDate.of(2025, 7, 10),
                LocalDate.of(2025, 7, 10),
                "개인 사정"
        );
        vacationBalance = VacationBalance.builder()
                .userId(userId)
                .totalGranted(BigDecimal.TEN)
                .usedDays(BigDecimal.ZERO)
                .build();
        userDetailDto = new UserDetailDto(userId, "홍길동", "개발팀");

        // 3) SUT에 mock 주입
        vacationService = new VacationService(
                vacationBalanceRepository,
                vacationRepository,
                approvalServiceClient,
                hrServiceClient
        );

        // 4) 공통 stubbing
        // — 연차 조회 시 기존 balance 반환
        when(vacationBalanceRepository.findByUserId(userId))
                .thenReturn(Optional.of(vacationBalance));
        // — HR 서비스 호출 시 userDetailDto 반환
        // when(hrServiceClient.getMyUserInfo())
        //         .thenReturn(userDetailDto);
        // — 결재 서비스 호출 시 예외 없이 처리
        // doNothing().when(approvalServiceClient)
        //         .createApproval(any(ApprovalRequestDto.class));
    }

    @Test
    @DisplayName("휴가 신청이 성공적으로 처리되어야 한다.")
    void requestVacation_success() {
        // Given
        // — balance 저장 시 그대로 리턴
        when(vacationBalanceRepository.save(any(VacationBalance.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        // — vacation 저장 시 그대로 리턴
        when(vacationRepository.save(any(Vacation.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        // HR 서비스 호출 시 userDetailDto 반환
        when(hrServiceClient.getMyUserInfo())
                .thenReturn(userDetailDto);
        // 결재 서비스 호출 시 예외 없이 처리
        doNothing().when(approvalServiceClient)
                .createApproval(any(ApprovalRequestDto.class));

        // when: 서비스 실행
        vacationService.requestVacation(userInfo, requestDto);

        // then: repository 조회/저장, 외부 서비스 호출 검증
        verify(vacationBalanceRepository).findByUserId(userId);
        verify(vacationRepository).save(any(Vacation.class));
        verify(vacationBalanceRepository).save(argThat(bal ->
                bal.getUsedDays().equals(BigDecimal.ONE) &&
                        bal.getRemainingDays().equals(BigDecimal.valueOf(9))
        ));
        verify(hrServiceClient).getMyUserInfo();
        verify(approvalServiceClient).createApproval(any(ApprovalRequestDto.class));
    }

    @Test
    @DisplayName("남은 연차가 부족하면 휴가 신청이 실패해야 한다.")
    void requestVacation_insufficientBalance_throwsException() {
        // given: 0일만 허용된 상태로 변경
        vacationBalance = VacationBalance.builder()
                .userId(userId)
                .totalGranted(BigDecimal.ZERO)
                .usedDays(BigDecimal.ZERO)
                .build();
        when(vacationBalanceRepository.findByUserId(userId))
                .thenReturn(Optional.of(vacationBalance));

        // when & then: IllegalStateException 발생
        assertThatThrownBy(() ->
                vacationService.requestVacation(userInfo, requestDto)
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("남은 연차가 부족합니다.");

        // 외부 호출이 전혀 일어나지 않아야 함
        verifyNoInteractions(vacationRepository, hrServiceClient, approvalServiceClient);
    }

    @Test
    @DisplayName("사용자의 연차 정보를 찾을 수 없으면 휴가 신청이 실패해야 한다.")
    void requestVacation_vacationBalanceNotFound_throwsException() {
        // given: 연차 정보가 없을 때
        when(vacationBalanceRepository.findByUserId(userId))
                .thenReturn(Optional.empty());

        // when & then: IllegalArgumentException 발생
        assertThatThrownBy(() ->
                vacationService.requestVacation(userInfo, requestDto)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 사용자의 연차 정보를 찾을 수 없습니다");

        // 전혀 호출이 일어나지 않아야 함
        verifyNoInteractions(vacationRepository, hrServiceClient, approvalServiceClient);
        verify(vacationBalanceRepository, never()).save(any());
    }

    @Test
    @DisplayName("사용자의 연차 현황을 성공적으로 조회해야 한다.")
    void getVacationBalance_success() {
        // Given
        // setUp에서 이미 vacationBalance가 설정되어 있고, findByUserId가 이를 반환하도록 stubbing 되어 있음

        // When
        VacationBalanceResDto result = vacationService.getVacationBalance(userId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getTotalGranted()).isEqualTo(BigDecimal.TEN);
        assertThat(result.getUsedDays()).isEqualTo(BigDecimal.ZERO);
        assertThat(result.getRemainingDays()).isEqualTo(BigDecimal.TEN);

        verify(vacationBalanceRepository).findByUserId(userId); // findByUserId가 호출되었는지 검증
    }

    @Test
    @DisplayName("연차 현황 조회 시 해당 사용자의 연차 정보를 찾을 수 없으면 예외를 발생시켜야 한다.")
    void getVacationBalance_vacationBalanceNotFound_throwsException() {
        // Given
        when(vacationBalanceRepository.findByUserId(userId))
                .thenReturn(Optional.empty()); // 연차 정보가 없는 상황 설정

        // When & Then
        assertThatThrownBy(() -> vacationService.getVacationBalance(userId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 사용자의 연차 정보를 찾을 수 없습니다.");

        verify(vacationBalanceRepository).findByUserId(userId); // findByUserId가 호출되었는지 검증
    }
}
