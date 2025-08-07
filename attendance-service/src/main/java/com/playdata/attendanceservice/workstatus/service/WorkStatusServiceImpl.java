package com.playdata.attendanceservice.workstatus.service;

import com.playdata.attendanceservice.absence.entity.Absence;
import com.playdata.attendanceservice.workstatus.entity.WorkStatus;
import com.playdata.attendanceservice.workstatus.entity.WorkStatusType;
import com.playdata.attendanceservice.workstatus.repository.WorkStatusRepository;
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
                // isLate는 기존 값을 유지합니다. 휴가/부재 승인으로 인한 업데이트 시에는 isLate를 변경하지 않습니다.

                // Attendance 엔티티가 연결되어 있다면 checkInTime과 checkOutTime을 업데이트
                if (workStatus.getAttendance() != null) {
                    workStatus.setCheckInTime(workStatus.getAttendance().getCheckInTime());
                    workStatus.setCheckOutTime(workStatus.getAttendance().getCheckOutTime());
                }
            } else {
                // 새로운 WorkStatus 생성
                workStatus = WorkStatus.builder()
                        .userId(userId)
                        .date(date)
                        .statusType(statusType)
                        .isLate(false) // 새로 생성되는 WorkStatus는 기본적으로 지각이 아님
                        .reason(reason)
                        .checkInTime(null) // 실제 출근 기록이 없으므로 null
                        .checkOutTime(null) // 실제 퇴근 기록이 없으므로 null
                        .build();
            }
            workStatusRepository.save(workStatus);
            log.info("WorkStatus updated for userId: {}, date: {}, type: {}", userId, date, statusType);
        }
    }
}
