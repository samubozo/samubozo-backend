package com.playdata.attendanceservice.absence.service;

import com.playdata.attendanceservice.absence.dto.request.AbsenceRequestDto;
import com.playdata.attendanceservice.absence.dto.request.AbsenceUpdateRequestDto;
import com.playdata.attendanceservice.absence.dto.response.AbsenceResponseDto;
import com.playdata.attendanceservice.absence.dto.response.AbsenceStatisticsDto;
import com.playdata.attendanceservice.absence.entity.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AbsenceService {

    AbsenceResponseDto createAbsence(Long userId, AbsenceRequestDto requestDto);

    AbsenceResponseDto updateAbsence(Long absenceId, AbsenceUpdateRequestDto requestDto, Long userId);

    List<AbsenceResponseDto> getAbsencesByUserId(Long userId);

    AbsenceResponseDto getAbsenceById(Long absenceId);

    void deleteAbsence(Long absenceId, Long userId);

    void approveAbsence(Long absenceId, Long approverId);

    void rejectAbsence(Long absenceId, Long approverId, String rejectComment);

    @Transactional(readOnly = true)
    Page<AbsenceResponseDto> getAbsencesForApproval(Pageable pageable);

    @Transactional(readOnly = true)
    Page<AbsenceResponseDto> getProcessedAbsences(Pageable pageable);

    @Transactional(readOnly = true)
    List<AbsenceResponseDto> getAbsencesByStatus(ApprovalStatus approvalStatus);

    @Transactional(readOnly = true)
    List<AbsenceResponseDto> getAbsencesByApprover(Long approverId);

    @Transactional(readOnly = true)
    List<AbsenceResponseDto> getAbsencesByUserIdAndStatus(Long userId, ApprovalStatus approvalStatus);

    @Transactional(readOnly = true)
    List<AbsenceResponseDto> getAbsencesByDateRange(String startDate, String endDate);

    @Transactional(readOnly = true)
    AbsenceStatisticsDto getAbsenceStatistics();
}
