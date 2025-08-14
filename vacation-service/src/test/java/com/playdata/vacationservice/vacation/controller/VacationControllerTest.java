package com.playdata.vacationservice.vacation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playdata.vacationservice.common.auth.TokenUserInfo;
import com.playdata.vacationservice.vacation.dto.VacationBalanceResDto;
import com.playdata.vacationservice.vacation.service.VacationService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VacationController.class) // Spring Security 필터 체인 활성화
class VacationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VacationService vacationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("연차 현황 조회 성공 시 200 OK와 연차 정보를 반환해야 한다.")
    void getVacationBalance_success() throws Exception {
        // Given
        Long userId = 1L;
        TokenUserInfo mockUserInfo = new TokenUserInfo("test@example.com", "ROLE_USER", userId);

        VacationBalanceResDto mockResponse = VacationBalanceResDto.builder()
                .userId(userId)
                .totalGranted(new BigDecimal("15.0"))
                .usedDays(new BigDecimal("5.0"))
                .remainingDays(new BigDecimal("10.0"))
                .build();

        when(vacationService.getVacationBalance(anyLong()))
                .thenReturn(mockResponse);

        // When & Then
        mockMvc.perform(get("/vacations/balance")
                        .with(SecurityMockMvcRequestPostProcessors.user(mockUserInfo))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("연차 현황 조회 성공"))
                .andExpect(jsonPath("$.result.userId").value(userId))
                .andExpect(jsonPath("$.result.totalGranted").value(15.0))
                .andExpect(jsonPath("$.result.usedDays").value(5.0))
                .andExpect(jsonPath("$.result.remainingDays").value(10.0));
    }

    @Test
    @DisplayName("연차 현황 조회 시 연차 정보를 찾을 수 없으면 404 Not Found를 반환해야 한다.")
    void getVacationBalance_notFound() throws Exception {
        // Given
        Long userId = 1L;
        TokenUserInfo mockUserInfo = new TokenUserInfo("test@example.com", "ROLE_USER", userId);

        when(vacationService.getVacationBalance(anyLong()))
                .thenThrow(new EntityNotFoundException("해당 사용자의 연차 정보를 찾을 수 없습니다."));

        // When & Then
        mockMvc.perform(get("/vacations/balance")
                        .with(SecurityMockMvcRequestPostProcessors.user(mockUserInfo))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("해당 사용자의 연차 정보를 찾을 수 없습니다."));
    }

    @Test
    @DisplayName("연차 현황 조회 시 내부 서버 오류 발생 시 500 Internal Server Error를 반환해야 한다.")
    void getVacationBalance_internalServerError() throws Exception {
        // Given
        Long userId = 1L;
        TokenUserInfo mockUserInfo = new TokenUserInfo("test@example.com", "ROLE_USER", userId);

        when(vacationService.getVacationBalance(anyLong()))
                .thenThrow(new RuntimeException("데이터베이스 연결 오류"));

        // When & Then
        mockMvc.perform(get("/vacations/balance")
                        .with(SecurityMockMvcRequestPostProcessors.user(mockUserInfo))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.message").value("연차 현황 조회 중 오류 발생"));
    }
}