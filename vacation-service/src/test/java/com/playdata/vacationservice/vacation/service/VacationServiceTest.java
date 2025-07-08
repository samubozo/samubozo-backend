package com.playdata.vacationservice.vacation.service; // 패키지 변경

import com.playdata.vacationservice.vacation.dto.VacationRequestDto;
import com.playdata.vacationservice.vacation.entity.Vacation;
import com.playdata.vacationservice.vacation.entity.VacationBalance;
import com.playdata.vacationservice.vacation.entity.VacationType;
import com.playdata.vacationservice.vacation.repository.VacationBalanceRepository;
import com.playdata.vacationservice.vacation.repository.VacationRepository;
import com.playdata.vacationservice.client.ApprovalServiceClient;
import com.playdata.vacationservice.client.dto.ApprovalRequestDto;
import com.playdata.vacationservice.client.dto.UserDetailDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock; // 추가
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer; // 추가

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VacationServiceTest {

    @Mock
    private VacationBalanceRepository vacationBalanceRepository;
    @Mock
    private VacationRepository vacationRepository;
    @Mock
    private ApprovalServiceClient approvalServiceClient;
    @Mock
    private HrServiceClient hrServiceClient;

    @InjectMocks
    private VacationService vacationService;

    private Long userId;
    private VacationRequestDto requestDto;
    private VacationBalance vacationBalance;
    private UserDetailDto userDetailDto;

    @BeforeEach
    void setUp() {
        userId = 1L;
        requestDto = new VacationRequestDto(VacationType.ANNUAL_LEAVE, LocalDate.of(2025, 7, 10), LocalDate.of(2025, 7, 10), "개인 사정");
        vacationBalance = VacationBalance.builder()
                .userId(userId)
                .totalGranted(BigDecimal.TEN)
                .usedDays(BigDecimal.ZERO)
                .build();
        userDetailDto = new UserDetailDto(userId, "홍길동", "개발팀");
    }

    @DisplayName("휴가 신청이 성공적으로 처리되어야 한다.")
    @Test
    void requestVacation_success() {
        // Given
        when(vacationBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(vacationBalance));
        when(hrServiceClient.getUserDetails(userId)).thenReturn(userDetailDto);

        // Use doAnswer to set the ID on the Vacation object passed to save
        doAnswer(new Answer<Void>() {
            public Void answer(InvocationOnMock invocation) {
                Vacation vacation = invocation.getArgument(0);
                // Use reflection to set the private 'id' field
                try {
                    java.lang.reflect.Field idField = Vacation.class.getDeclaredField("id");
                    idField.setAccessible(true);
                    idField.set(vacation, 1L); // Set a dummy ID
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    throw new RuntimeException("Failed to set ID on Vacation object during test", e);
                }
                return null;
            }
        }).when(vacationRepository).save(any(Vacation.class));

        // When
        vacationService.requestVacation(userId, requestDto);

        // Then
        verify(vacationBalanceRepository, times(1)).findByUserId(userId);
        verify(vacationRepository, times(1)).save(any(Vacation.class));
        verify(hrServiceClient, times(1)).getUserDetails(userId);
        verify(approvalServiceClient, times(1)).createApproval(any(ApprovalRequestDto.class));
        verify(vacationBalanceRepository, times(1)).save(vacationBalance);

        assertThat(vacationBalance.getUsedDays()).isEqualTo(BigDecimal.ONE); // 연차 1일 차감 확인
        assertThat(vacationBalance.getRemainingDays()).isEqualTo(BigDecimal.valueOf(9)); // 남은 연차 확인

        ArgumentCaptor<ApprovalRequestDto> approvalCaptor = ArgumentCaptor.forClass(ApprovalRequestDto.class);
        verify(approvalServiceClient).createApproval(approvalCaptor.capture());
        ApprovalRequestDto capturedApproval = approvalCaptor.getValue();

        assertThat(capturedApproval.getApprovalType()).isEqualTo("VACATION");
        assertThat(capturedApproval.getUserId()).isEqualTo(userId);
        assertThat(capturedApproval.getTitle()).contains("홍길동", "개발팀", "2025-07-10");
        assertThat(capturedApproval.getContent()).contains("신청자: 홍길동 (개발팀)", "휴가 종류: 연차", "기간: 2025-07-10 ~ 2025-07-10", "사유: 개인 사정");
        assertThat(capturedApproval.getReferenceId()).isEqualTo(1L); // Vacation ID가 설정되었는지 확인
    }

    @DisplayName("남은 연차가 부족하면 휴가 신청이 실패해야 한다.")
    @Test
    void requestVacation_insufficientBalance_throwsException() {
        // Given
        vacationBalance = VacationBalance.builder()
                .userId(userId)
                .totalGranted(BigDecimal.ZERO)
                .usedDays(BigDecimal.ZERO)
                .build(); // 남은 연차 0일
        when(vacationBalanceRepository.findByUserId(userId)).thenReturn(Optional.of(vacationBalance));

        // When & Then
        assertThatThrownBy(() -> vacationService.requestVacation(userId, requestDto))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("남은 연차가 부족합니다.");

        verify(vacationRepository, never()).save(any(Vacation.class));
        verify(hrServiceClient, never()).getUserDetails(anyLong());
        verify(approvalServiceClient, never()).createApproval(any(ApprovalRequestDto.class));
        verify(vacationBalanceRepository, never()).save(vacationBalance);
    }

    @DisplayName("사용자의 연차 정보를 찾을 수 없으면 휴가 신청이 실패해야 한다.")
    @Test
    void requestVacation_vacationBalanceNotFound_throwsException() {
        // Given
        when(vacationBalanceRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> vacationService.requestVacation(userId, requestDto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 사용자의 연차 정보를 찾을 수 없습니다");

        verify(vacationRepository, never()).save(any(Vacation.class));
        verify(hrServiceClient, never()).getUserDetails(anyLong());
        verify(approvalServiceClient, never()).createApproval(any(ApprovalRequestDto.class));
        verify(vacationBalanceRepository, never()).save(any(VacationBalance.class));
    }
}