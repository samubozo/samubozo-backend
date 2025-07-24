package com.playdata.attendanceservice.attendance.absence.service;

import com.playdata.attendanceservice.attendance.absence.dto.request.AbsenceRequestDto;
import com.playdata.attendanceservice.attendance.absence.dto.request.AbsenceUpdateRequestDto;
import com.playdata.attendanceservice.attendance.absence.dto.response.AbsenceResponseDto;
import com.playdata.attendanceservice.attendance.absence.entity.Absence;
import com.playdata.attendanceservice.attendance.absence.repository.AbsenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AbsenceService {

    private final AbsenceRepository absenceRepository;

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
        absenceRepository.save(absence);
        log.info("Absence saved successfully for userId: {}", userId);
    }

    /**
     * 특정 부재 정보를 업데이트합니다.
     * @param absenceId 업데이트할 부재 ID
     * @param requestDto 부재 업데이트 요청 DTO
     * @return 업데이트된 부재 정보 (AbsenceResponseDto)
     */
    @Transactional
    public AbsenceResponseDto updateAbsence(Long absenceId, AbsenceUpdateRequestDto requestDto) {
        Absence absence = absenceRepository.findById(absenceId)
                .orElseThrow(() -> new IllegalArgumentException("Absence not found with id: " + absenceId));

        absence.update(
                requestDto.getType(),
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
        List<Absence> absences = absenceRepository.findByUserId(userId);
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
                .orElseThrow(() -> new IllegalArgumentException("Absence not found with id: " + absenceId));
        return AbsenceResponseDto.from(absence);
    }

    /**
     * 부재 삭제
     * @param absenceId 부재 ID
     */
    @Transactional
    public void deleteAbsence(Long absenceId) {
        Absence absence = absenceRepository.findById(absenceId)
                .orElseThrow(() -> new IllegalArgumentException("Absence not found with id: " + absenceId));
        absenceRepository.delete(absence);
        log.info("Absence deleted: {}", absenceId);
    }
}