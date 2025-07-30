package com.playdata.attendanceservice.attendance.absence.service;

import com.playdata.attendanceservice.attendance.absence.dto.request.AbsenceRequestDto;
import com.playdata.attendanceservice.attendance.absence.dto.request.AbsenceUpdateRequestDto;
import com.playdata.attendanceservice.attendance.absence.dto.response.AbsenceResponseDto;
import com.playdata.attendanceservice.attendance.absence.dto.response.AbsenceStatisticsDto;
import com.playdata.attendanceservice.attendance.absence.entity.Absence;
import com.playdata.attendanceservice.attendance.absence.entity.ApprovalStatus;
import com.playdata.attendanceservice.attendance.absence.repository.AbsenceRepository;
import com.playdata.attendanceservice.client.ApprovalServiceClient;
import com.playdata.attendanceservice.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbsenceService {

    private final AbsenceRepository absenceRepository;
    private final ApprovalServiceClient approvalServiceClient; // 전자결재 시스템 연동

    /**
     * 새로운 부재 정보를 등록합니다.
     * @param userId 부재를 등록할 사용자 ID
     * @param requestDto 부재 등록 요청 DTO
     */
    @Transactional
    public void createAbsence(String userId, AbsenceRequestDto requestDto) {
        log.info("Creating absence for userId: {}, requestDto: {}", userId, requestDto);

        Absence absence = requestDto.toEntity(userId);
        log.info("Converted to Absence entity: {}", absence);

        // 부재 저장
        Absence savedAbsence = absenceRepository.save(absence);
        log.info("Absence saved successfully for userId: {}, absenceId: {}", userId, savedAbsence.getId());

        // 전자결재가 필요한 부재인 경우 자동으로 결재 요청 생성
        if (requestDto.requiresApproval()) {
            try {
                log.info("Creating approval request for absence: {}", savedAbsence.getId());
                approvalServiceClient.createAbsenceApprovalRequest(savedAbsence);
                log.info("Approval request created successfully for absence: {}", savedAbsence.getId());
            } catch (Exception e) {
                log.error("Failed to create approval request for absence: {}", savedAbsence.getId(), e);
                // 결재 요청 실패해도 부재 등록은 성공으로 처리
            }
        }
    }

    /**
     * 특정 부재 정보를 업데이트합니다.
     * @param absenceId 업데이트할 부재 ID
     * @param requestDto 부재 업데이트 요청 DTO
     * @param userId 수정 요청한 사용자 ID
     * @return 업데이트된 부재 정보 (AbsenceResponseDto)
     */
    @Transactional
    public AbsenceResponseDto updateAbsence(Long absenceId, AbsenceUpdateRequestDto requestDto, String userId) {
        Absence absence = absenceRepository.findById(absenceId)
                .orElseThrow(() -> new BusinessException("부재를 찾을 수 없습니다: " + absenceId));

        // 대기 상태인 경우만 수정 가능
        if (!absence.isPending()) {
            throw new BusinessException("처리된 부재는 수정할 수 없습니다.");
        }

        // 본인이 신청한 부재만 수정 가능
        if (!absence.getUserId().equals(userId)) {
            throw new BusinessException("본인이 신청한 부재만 수정할 수 있습니다.");
        }

        absence.update(
                requestDto.getType(),
                requestDto.getUrgency(),
                requestDto.getStartDate(),
                requestDto.getEndDate(),
                requestDto.getStartTime(),
                requestDto.getEndTime(),
                requestDto.getReason()
        );

        log.info("Absence updated: {}", absence);
        return AbsenceResponseDto.from(absence);
    }

    /**
     * 특정 사용자의 모든 부재 내역을 조회합니다.
     * @param userId 사용자 ID
     * @return 해당 사용자의 부재 내역 리스트 (AbsenceResponseDto)
     */
    @Transactional(readOnly = true)
    public List<AbsenceResponseDto> getAbsencesByUserId(String userId) {
        List<Absence> absences = absenceRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return absences.stream()
                .map(AbsenceResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 단일 부재 상세 조회
     * @param absenceId 부재 ID
     * @return AbsenceResponseDto
     */
    @Transactional(readOnly = true)
    public AbsenceResponseDto getAbsenceById(Long absenceId) {
        Absence absence = absenceRepository.findById(absenceId)
                .orElseThrow(() -> new BusinessException("부재를 찾을 수 없습니다: " + absenceId));
        return AbsenceResponseDto.from(absence);
    }

    /**
     * 부재 삭제
     * @param absenceId 부재 ID
     * @param userId 삭제 요청한 사용자 ID
     */
    @Transactional
    public void deleteAbsence(Long absenceId, String userId) {
        Absence absence = absenceRepository.findById(absenceId)
                .orElseThrow(() -> new BusinessException("부재를 찾을 수 없습니다: " + absenceId));

        // 대기 상태인 경우만 삭제 가능
        if (!absence.isPending()) {
            throw new BusinessException("처리된 부재는 삭제할 수 없습니다.");
        }

        // 본인이 신청한 부재만 삭제 가능
        if (!absence.getUserId().equals(userId)) {
            throw new BusinessException("본인이 신청한 부재만 삭제할 수 있습니다.");
        }

        absenceRepository.delete(absence);
        log.info("Absence deleted: {}", absenceId);
    }

    /**
     * 부재 승인 (HR용)
     * @param absenceId 부재 ID
     * @param approverId 결재자 ID
     */
    @Transactional
    public void approveAbsence(Long absenceId, String approverId) {
        Absence absence = absenceRepository.findById(absenceId)
                .orElseThrow(() -> new BusinessException("부재를 찾을 수 없습니다: " + absenceId));

        if (!absence.isPending()) {
            throw new BusinessException("대기 상태인 부재만 승인할 수 있습니다.");
        }

        absence.approve(approverId);
        absenceRepository.save(absence);

        log.info("Absence approved: absenceId={}, approverId={}", absenceId, approverId);
    }

    /**
     * 부재 반려 (HR용)
     * @param absenceId 부재 ID
     * @param approverId 결재자 ID
     * @param rejectComment 반려 사유
     */
    @Transactional
    public void rejectAbsence(Long absenceId, String approverId, String rejectComment) {
        Absence absence = absenceRepository.findById(absenceId)
                .orElseThrow(() -> new BusinessException("부재를 찾을 수 없습니다: " + absenceId));

        if (!absence.isPending()) {
            throw new BusinessException("대기 상태인 부재만 반려할 수 있습니다.");
        }

        absence.reject(approverId, rejectComment);
        absenceRepository.save(absence);

        log.info("Absence rejected: absenceId={}, approverId={}", absenceId, approverId);
    }

    /**
     * 결재용 부재 목록 조회 (대기 중)
     * @param pageable 페이지 정보
     * @return 대기 중인 부재 목록
     */
    @Transactional(readOnly = true)
    public Page<AbsenceResponseDto> getAbsencesForApproval(Pageable pageable) {
        Page<Absence> absences = absenceRepository.findPendingAbsences(pageable);
        return absences.map(AbsenceResponseDto::from);
    }

    /**
     * 처리된 부재 목록 조회
     * @param pageable 페이지 정보
     * @return 처리된 부재 목록
     */
    @Transactional(readOnly = true)
    public Page<AbsenceResponseDto> getProcessedAbsences(Pageable pageable) {
        Page<Absence> absences = absenceRepository.findProcessedAbsences(pageable);
        return absences.map(AbsenceResponseDto::from);
    }

    /**
     * 결재 상태별 부재 목록 조회
     * @param approvalStatus 결재 상태
     * @return 해당 상태의 부재 목록
     */
    @Transactional(readOnly = true)
    public List<AbsenceResponseDto> getAbsencesByStatus(ApprovalStatus approvalStatus) {
        List<Absence> absences = absenceRepository.findByApprovalStatusOrderByCreatedAtDesc(approvalStatus);
        return absences.stream()
                .map(AbsenceResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 결재자별 처리한 부재 목록 조회
     * @param approverId 결재자 ID
     * @return 해당 결재자가 처리한 부재 목록
     */
    @Transactional(readOnly = true)
    public List<AbsenceResponseDto> getAbsencesByApprover(String approverId) {
        List<Absence> absences = absenceRepository.findByApproverIdOrderByApprovedAtDesc(approverId);
        return absences.stream()
                .map(AbsenceResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자의 결재 상태별 부재 목록 조회
     * @param userId 사용자 ID
     * @param approvalStatus 결재 상태
     * @return 해당 사용자의 해당 상태 부재 목록
     */
    @Transactional(readOnly = true)
    public List<AbsenceResponseDto> getAbsencesByUserIdAndStatus(String userId, ApprovalStatus approvalStatus) {
        List<Absence> absences = absenceRepository.findByUserIdAndApprovalStatusOrderByCreatedAtDesc(userId, approvalStatus);
        return absences.stream()
                .map(AbsenceResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 날짜 범위별 부재 목록 조회
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 해당 기간의 부재 목록
     */
    @Transactional(readOnly = true)
    public List<AbsenceResponseDto> getAbsencesByDateRange(String startDate, String endDate) {
        List<Absence> absences = absenceRepository.findByDateRange(
                LocalDateTime.parse(startDate).toLocalDate(),
                LocalDateTime.parse(endDate).toLocalDate()
        );
        return absences.stream()
                .map(AbsenceResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 부재 통계 조회
     * @return 부재 통계 정보
     */
    @Transactional(readOnly = true)
    public AbsenceStatisticsDto getAbsenceStatistics() {
        long totalAbsences = absenceRepository.count();
        long pendingAbsences = absenceRepository.countByApprovalStatus(ApprovalStatus.PENDING);
        long approvedAbsences = absenceRepository.countByApprovalStatus(ApprovalStatus.APPROVED);
        long rejectedAbsences = absenceRepository.countByApprovalStatus(ApprovalStatus.REJECTED);

        return AbsenceStatisticsDto.builder()
                .totalAbsences(totalAbsences)
                .pendingAbsences(pendingAbsences)
                .approvedAbsences(approvedAbsences)
                .rejectedAbsences(rejectedAbsences)
                .build();
    }
}