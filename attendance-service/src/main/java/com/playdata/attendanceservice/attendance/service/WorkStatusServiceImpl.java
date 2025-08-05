package com.playdata.attendanceservice.attendance.service;

import com.playdata.attendanceservice.attendance.absence.entity.Absence;
import com.playdata.attendanceservice.attendance.entity.WorkStatus;
import com.playdata.attendanceservice.attendance.entity.WorkStatusType;
import com.playdata.attendanceservice.attendance.repository.WorkStatusRepository;
import com.playdata.attendanceservice.client.dto.VacationWorkStatusRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WorkStatusServiceImpl implements WorkStatusService {

    private final WorkStatusRepository workStatusRepository;

    @Override
    public void createWorkStatusForAbsence(Absence absence) {
        log.info("Updating WorkStatus for absence. absenceId: {}", absence.getId());
        WorkStatusType workStatusType = absence.getType().toWorkStatusType();
        updateWorkStatus(absence.getUserId(), absence.getStartDate(), absence.getEndDate(), workStatusType, absence.getReason());
    }

    @Override
    public void createWorkStatusForVacation(VacationWorkStatusRequestDto requestDto) {
        log.info("Updating WorkStatus for vacation. userId: {}, startDate: {}, endDate: {}",
                requestDto.getUserId(), requestDto.getStartDate(), requestDto.getEndDate());
        WorkStatusType workStatusType = WorkStatusType.valueOf(requestDto.getVacationType()); // 문자열을 Enum으로 변환
        updateWorkStatus(requestDto.getUserId(), requestDto.getStartDate(), requestDto.getEndDate(), workStatusType, requestDto.getReason());
    }

    private void updateWorkStatus(Long userId, LocalDate startDate, LocalDate endDate, WorkStatusType statusType, String reason) {
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            Optional<WorkStatus> existingStatus = workStatusRepository.findByUserIdAndDate(userId, date);

            WorkStatus workStatus;
            if (existingStatus.isPresent()) {
                workStatus = existingStatus.get();
                // 기존 WorkStatus 업데이트
                workStatus.setStatusType(statusType);
                workStatus.setReason(reason);
                workStatus.setIsLate(false); // 휴가/부재는 지각이 아님
            } else {
                // 새로운 WorkStatus 생성
                workStatus = WorkStatus.builder()
                        .userId(userId)
                        .date(date)
                        .statusType(statusType)
                        .isLate(false)
                        .reason(reason)
                        .build();
            }
            workStatusRepository.save(workStatus);
            log.info("WorkStatus updated for userId: {}, date: {}, type: {}", userId, date, statusType);
        }
    }
}
