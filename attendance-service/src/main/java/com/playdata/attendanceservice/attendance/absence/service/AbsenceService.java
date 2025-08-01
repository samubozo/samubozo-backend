package com.playdata.attendanceservice.attendance.absence.service;

import com.playdata.attendanceservice.attendance.absence.dto.request.AbsenceRequestDto;
import com.playdata.attendanceservice.attendance.absence.dto.request.AbsenceApprovalRequestCreateDto;
import com.playdata.attendanceservice.attendance.absence.dto.request.AbsenceUpdateRequestDto;
import com.playdata.attendanceservice.attendance.absence.dto.request.ApprovalRequestDto;
import com.playdata.attendanceservice.attendance.absence.dto.response.AbsenceResponseDto;
import com.playdata.attendanceservice.attendance.absence.dto.response.AbsenceStatisticsDto;
import com.playdata.attendanceservice.attendance.absence.entity.Absence;
import com.playdata.attendanceservice.attendance.absence.entity.ApprovalStatus;
import com.playdata.attendanceservice.attendance.absence.repository.AbsenceRepository;
import com.playdata.attendanceservice.attendance.entity.WorkStatus;
import com.playdata.attendanceservice.attendance.repository.WorkStatusRepository;
import com.playdata.attendanceservice.client.ApprovalServiceClient;
import com.playdata.attendanceservice.common.exception.BusinessException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbsenceService {

    private final AbsenceRepository absenceRepository;
    private final ApprovalServiceClient approvalServiceClient;
    private final WorkStatusRepository workStatusRepository;

    /**
     * 새로운 부재 정보를 등록합니다.
     * @param userId 부재를 등록할 사용자 ID (Long)
     * @param requestDto 부재 등록 요청 DTO
     */
    @Transactional
    public AbsenceResponseDto createAbsence(Long userId, AbsenceRequestDto requestDto) {
        log.info("Creating absence for userId: {}, requestDto: {}", userId, requestDto);

        // 1. Absence 엔티티 생성 및 DB에 저장
        Absence absence = requestDto.toEntity(userId);
        Absence savedAbsence = absenceRepository.save(absence);
        log.info("Absence saved successfully for userId: {}, absenceId: {}", userId, savedAbsence.getId());

        // 2. 전자결재가 필요한 부재인 경우, 결재 요청 생성
        if (requestDto.requiresApproval()) {
            log.info("Creating approval request for absence: {}", savedAbsence.getId());

            // approval-service에 맞는 DTO로 변환
            AbsenceApprovalRequestCreateDto approvalDto = AbsenceApprovalRequestCreateDto.builder()
                    .absencesId(savedAbsence.getId())
                    .absenceType(savedAbsence.getType())
                    .urgency(savedAbsence.getUrgency())
                    .startDate(savedAbsence.getStartDate())
                    .endDate(savedAbsence.getEndDate())
                    .startTime(savedAbsence.getStartTime())
                    .endTime(savedAbsence.getEndTime())
                    .reason(savedAbsence.getReason())
                    .build();

            // Feign Client를 통해 approval-service 호출
            try {
                approvalServiceClient.createAbsenceApprovalRequest(approvalDto);
                log.info("Approval request created successfully for absence: {}", savedAbsence.getId());
            } catch (FeignException e) {
                log.error("Approval service call failed for absenceId: {}. Exception: {}", savedAbsence.getId(), e.getMessage());
                // Feign 호출 실패 시, AbsenceService의 트랜잭션을 롤백
                throw new BusinessException("결재 요청 실패: " + e.getMessage(), e);
            }
        }
        return AbsenceResponseDto.from(savedAbsence);
    }

    /**
     * 특정 부재 정보를 업데이트합니다.
     * @param absenceId 업데이트할 부재 ID
     * @param requestDto 부재 업데이트 요청 DTO
     * @param userId 수정 요청한 사용자 ID (Long)
     * @return 업데이트된 부재 정보 (AbsenceResponseDto)
     */
    @Transactional
    public AbsenceResponseDto updateAbsence(Long absenceId, AbsenceUpdateRequestDto requestDto, Long userId) {
        Absence absence = absenceRepository.findById(absenceId)
                .orElseThrow(() -> new BusinessException("부재를 찾을 수 없습니다: " + absenceId));

        // 대기 상태인 경우만 수정 가능
        if (!absence.isPending()) {
            throw new BusinessException("처리된 부재는 수정할 수 없습니다.");
        }

        // 본인이 신청한 부재만 수정 가능 (Long 비교)
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
     * @param userId 사용자 ID (Long)
     * @return 해당 사용자의 부재 내역 리스트 (AbsenceResponseDto)
     */
    @Transactional(readOnly = true)
    public List<AbsenceResponseDto> getAbsencesByUserId(Long userId) {
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
     * @param userId 삭제 요청한 사용자 ID (Long)
     */
    @Transactional
    public void deleteAbsence(Long absenceId, Long userId) {
        Absence absence = absenceRepository.findById(absenceId)
                .orElseThrow(() -> new BusinessException("부재를 찾을 수 없습니다: " + absenceId));

        // 대기 상태인 경우만 삭제 가능
        if (!absence.isPending()) {
            throw new BusinessException("처리된 부재는 삭제할 수 없습니다.");
        }

        // 본인이 신청한 부재만 삭제 가능 (Long 비교)
        if (!absence.getUserId().equals(userId)) {
            throw new BusinessException("본인이 신청한 부재만 삭제할 수 있습니다.");
        }

        absenceRepository.delete(absence);
        log.info("Absence deleted: {}", absenceId);
    }

    /**
     * 부재 승인 (HR용) - WorkStatus 업데이트 포함
     * @param absenceId 부재 ID
     * @param approverId 결재자 ID (Long)
     */
    @Transactional
    public void approveAbsence(Long absenceId, Long approverId) {
        log.info("Approving absence: absenceId={}, approverId={}", absenceId, approverId);

        Absence absence = absenceRepository.findById(absenceId)
                .orElseThrow(() -> new BusinessException("부재를 찾을 수 없습니다: " + absenceId));

        if (!absence.isPending()) {
            throw new BusinessException("대기 상태인 부재만 승인할 수 있습니다.");
        }

        absence.approve(approverId); // Long 타입으로 직접 전달
        absenceRepository.save(absence);

        log.info("Absence approved successfully: absenceId={}", absenceId);

        // WorkStatus 업데이트
        updateWorkStatusForAbsence(absence);

        log.info("Absence approval process completed: absenceId={}, approverId={}", absenceId, approverId);
    }

    /**
     * 부재 승인 시 WorkStatus 업데이트
     * @param absence 승인된 부재 엔티티
     */
    private void updateWorkStatusForAbsence(Absence absence) {
        LocalDate startDate = absence.getStartDate();
        LocalDate endDate = absence.getEndDate();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Optional<WorkStatus> existingStatus = workStatusRepository
                    .findByUserIdAndDate(absence.getUserId(), date);

            WorkStatus workStatus;
            if (existingStatus.isPresent()) {
                workStatus = existingStatus.get();
                // 기존 WorkStatus 업데이트
                workStatus.setStatusType(absence.getType().toWorkStatusType());
                workStatus.setIsLate(false);
                workStatus.setReason(absence.getReason());
            } else {
                // 새로운 WorkStatus 생성
                workStatus = WorkStatus.builder()
                        .userId(absence.getUserId())
                        .date(date)
                        .statusType(absence.getType().toWorkStatusType())
                        .isLate(false)
                        .reason(absence.getReason())
                        .build();
            }

            workStatusRepository.save(workStatus);
            log.info("WorkStatus updated for userId: {}, date: {}, type: {}",
                    absence.getUserId(), date, absence.getType());
        }
    }

    /**
     * 부재 반려 (HR용)
     * @param absenceId 부재 ID
     * @param approverId 결재자 ID (Long)
     * @param rejectComment 반려 사유
     */
    @Transactional
    public void rejectAbsence(Long absenceId, Long approverId, String rejectComment) {
        log.info("Rejecting absence: absenceId={}, approverId={}", absenceId, approverId);

        Absence absence = absenceRepository.findById(absenceId)
                .orElseThrow(() -> new BusinessException("부재를 찾을 수 없습니다: " + absenceId));

        if (!absence.isPending()) {
            throw new BusinessException("대기 상태인 부재만 반려할 수 있습니다.");
        }

        absence.reject(approverId, rejectComment); // Long 타입으로 직접 전달
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
     * @param approverId 결재자 ID (Long)
     * @return 해당 결재자가 처리한 부재 목록
     */
    @Transactional(readOnly = true)
    public List<AbsenceResponseDto> getAbsencesByApprover(Long approverId) {
        List<Absence> absences = absenceRepository.findByApproverIdOrderByApprovedAtDesc(approverId);
        return absences.stream()
                .map(AbsenceResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 사용자의 결재 상태별 부재 목록 조회
     * @param userId 사용자 ID (Long)
     * @param approvalStatus 결재 상태
     * @return 해당 사용자의 해당 상태 부재 목록
     */
    @Transactional(readOnly = true)
    public List<AbsenceResponseDto> getAbsencesByUserIdAndStatus(Long userId, ApprovalStatus approvalStatus) {
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