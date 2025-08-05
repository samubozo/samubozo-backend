package com.playdata.attendanceservice.attendance.service;

import com.playdata.attendanceservice.attendance.dto.AttendanceResDto;
import com.playdata.attendanceservice.attendance.dto.PersonalAttendanceStatsDto;
import com.playdata.attendanceservice.attendance.dto.WorkTimeDto;
import com.playdata.attendanceservice.attendance.entity.Attendance;
import com.playdata.attendanceservice.attendance.entity.WorkDayType;
import com.playdata.attendanceservice.workstatus.entity.WorkStatus;
import com.playdata.attendanceservice.workstatus.entity.WorkStatusType;
import com.playdata.attendanceservice.attendance.repository.AttendanceRepository;
import com.playdata.attendanceservice.workstatus.repository.WorkStatusRepository;
import com.playdata.attendanceservice.client.ApprovalServiceClient;
import com.playdata.attendanceservice.client.HrServiceClient;
import com.playdata.attendanceservice.client.VacationServiceClient;
import com.playdata.attendanceservice.client.dto.MonthlyVacationStatsDto;
import com.playdata.attendanceservice.common.dto.CommonResDto;
import com.playdata.attendanceservice.common.exception.AttendanceAlreadyExistsException;
import com.playdata.attendanceservice.common.exception.CheckInNotFoundException;
import com.playdata.attendanceservice.common.exception.AlreadyCheckedOutException;
import com.playdata.attendanceservice.common.exception.AlreadyOnLeaveException;
import com.playdata.attendanceservice.common.constants.WorkConstants; // WorkConstants import 추가
import com.playdata.attendanceservice.common.util.WorkTimeCalculator; // WorkTimeCalculator import로 변경
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final WorkStatusRepository workStatusRepository;
    private final HrServiceClient hrServiceClient;
    private final VacationServiceClient vacationServiceClient;
    private final ApprovalServiceClient approvalServiceClient;

    @Value("${standard.checkin.time}")
    private String standardCheckInTimeStr;

    @Value("${standard.checkout.time}")
    private String standardCheckOutTimeStr;

    @Override
    @Transactional
    public Attendance recordCheckIn(Long userId, String ipAddress) {
        return recordCheckIn(userId, ipAddress, LocalDateTime.now());
    }

    @Override
    @Transactional
    public Attendance recordCheckIn(Long userId, String ipAddress, LocalDateTime checkInDateTime) {
        // 1. 이미 오늘 출근 기록이 있는지 확인
            attendanceRepository.findByUserIdAndAttendanceDate(userId, checkInDateTime.toLocalDate())
                    .ifPresent(attendance -> {
                        throw new AttendanceAlreadyExistsException("이미 오늘 출근 기록이 존재합니다. (User ID: " + userId + ", Date: " + checkInDateTime.toLocalDate() + ")");
                    });

            // 2. Attendance 엔티티 생성
            Attendance attendance = Attendance.builder()
                    .userId(userId)
                    .attendanceDate(checkInDateTime.toLocalDate())
                    .ipAddress(ipAddress)
                    .build();

            attendance.updateCheckInTime(checkInDateTime);

            // 3. 초기 WorkStatusType 및 지각 여부 결정
            Map<String, Object> statusInfo = determineInitialWorkStatus(userId, checkInDateTime);
            WorkStatusType initialStatusType = (WorkStatusType) statusInfo.get("statusType");
            boolean isLate = (boolean) statusInfo.get("isLate");

            // 4. WorkStatus 엔티티 생성 및 Attendance와 연결
            WorkStatus workStatus = WorkStatus.builder()
                    .userId(userId)
                    .date(checkInDateTime.toLocalDate())
                    .statusType(initialStatusType)
                    .reason(null) // 초기에는 사유 없음
                    .checkInTime(checkInDateTime.toLocalTime())
                    .isLate(isLate)
                    .build();

            attendance.setWorkStatus(workStatus); // Attendance와 WorkStatus 양방향 연결

            try {
                // 5. Attendance 및 WorkStatus 저장
                Attendance savedAttendance = attendanceRepository.save(attendance);
                workStatusRepository.save(workStatus); // WorkStatus는 Attendance 저장 시 Cascade로 저장되지만, 명시적으로 저장하여 관계를 확실히 합니다.

                log.info("출근 기록 및 WorkStatus 저장 완료. Attendance ID: {}, User ID: {}, CheckInTime: {}, WorkStatusType: {}",
                        savedAttendance.getId(), userId, savedAttendance.getCheckInTime(), initialStatusType);

                return savedAttendance;
            } catch (DataIntegrityViolationException e) {
                log.warn("출근 기록 중복 시도 감지. User ID: {}, Date: {}. 에러: {}", userId, checkInDateTime.toLocalDate(), e.getMessage());
                throw new AttendanceAlreadyExistsException("이미 오늘 출근 기록이 존재합니다. (User ID: " + userId + ", Date: " + checkInDateTime.toLocalDate() + ")", e);
            }
    }

    /**
     * 사용자의 초기 근무 상태(WorkStatusType) 및 지각 여부를 결정합니다.
     * 외부 스케줄(출장, 병가 등) 또는 승인된 휴가(연차, 반차)가 있는지 확인하고,
     * 해당 사항이 없으면 출근 시간 기준으로 지각 여부를 판단합니다.
     *
     * @param userId 사용자 ID
     * @param checkInDateTime 출근 시각 (LocalDateTime)
     * @return WorkStatusType과 isLate를 포함하는 Map
     */
    private Map<String, Object> determineInitialWorkStatus(Long userId, LocalDateTime checkInDateTime) {
        WorkStatusType initialStatusType = WorkStatusType.REGULAR;
        boolean isLate = false;
        LocalDate today = checkInDateTime.toLocalDate();
        LocalTime currentCheckInTime = checkInDateTime.toLocalTime();
        LocalTime standardCheckInTime = LocalTime.parse(standardCheckInTimeStr);

        // 1. HR 서비스에서 승인된 외부 스케줄(출장, 병가 등) 확인
        String externalScheduleType = hrServiceClient.getApprovedExternalScheduleType(userId, today.format(DateTimeFormatter.ISO_LOCAL_DATE));
        if (externalScheduleType != null) {
            log.debug("User {} has approved external schedule: {}", userId, externalScheduleType);
            switch (externalScheduleType) {
                case "BUSINESS_TRIP":
                    initialStatusType = WorkStatusType.BUSINESS_TRIP;
                    break;
                case "TRAINING":
                    initialStatusType = WorkStatusType.TRAINING;
                    break;
                case "SICK_LEAVE":
                    initialStatusType = WorkStatusType.SICK_LEAVE;
                    break;
                case "OFFICIAL_LEAVE":
                    initialStatusType = WorkStatusType.OFFICIAL_LEAVE;
                    break;
                case "SHORT_LEAVE":
                    initialStatusType = WorkStatusType.SHORT_LEAVE;
                    break;
                case "ETC":
                    initialStatusType = WorkStatusType.ETC;
                    break;
                default:
                    initialStatusType = WorkStatusType.ABSENCE; // 기타 부재
                    break;
            }
        } else {
            // 2. Approval 서비스에서 승인된 휴가(연차, 반차) 확인
            String approvedLeaveType = approvalServiceClient.getApprovedLeaveType(userId, today.format(DateTimeFormatter.ISO_LOCAL_DATE));
            if (approvedLeaveType != null) {
                log.debug("User {} has approved leave: {}", userId, approvedLeaveType);
                WorkStatusType leaveStatusType = getWorkStatusTypeFromApprovedLeaveType(approvedLeaveType);
                if (leaveStatusType != null) {
                    initialStatusType = leaveStatusType;
                } else {
                    // 알 수 없는 휴가 타입은 REGULAR로 처리하거나, 필요에 따라 예외 처리
                    log.warn("Unknown approved leave type for user {}: {}", userId, approvedLeaveType);
                }
            } else if (currentCheckInTime.isAfter(standardCheckInTime)) {
                // 3. 외부 스케줄이나 휴가가 없으면 지각 여부 판단
                log.debug("User {} is late. Check-in time: {}, Standard check-in time: {}", userId, currentCheckInTime, standardCheckInTime);
                initialStatusType = WorkStatusType.LATE;
                isLate = true;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("statusType", initialStatusType);
        result.put("isLate", isLate);
        return result;
    }

    @Override
    @Transactional
    public Attendance recordCheckOut(Long userId) {
        return recordCheckOut(userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public Attendance recordCheckOut(Long userId, LocalDateTime checkOutDateTime) {
        LocalDate today = checkOutDateTime.toLocalDate();
        // 1. 오늘 출근 기록이 있는지 확인
        Attendance attendance = attendanceRepository.findByUserIdAndAttendanceDate(userId, today)
                .orElseThrow(() -> new CheckInNotFoundException("오늘 출근 기록이 존재하지 않아 퇴근할 수 없습니다. (User ID: " + userId + ")"));

        // 2. 이미 퇴근 기록이 완료되었는지 확인
        if (attendance.getCheckOutTime() != null) {
            throw new AlreadyCheckedOutException("이미 퇴근 기록이 완료되었습니다. (User ID: " + userId + ")");
        }

        // 3. Attendance 엔티티에 퇴근 시간 업데이트
        attendance.updateCheckOutTime(checkOutDateTime);
        Attendance savedAttendance = attendanceRepository.save(attendance);
        log.info("Attendance 퇴근 시간 업데이트 및 저장 완료. Attendance ID: {}, User ID: {}, CheckOutTime: {}",
                savedAttendance.getId(), userId, savedAttendance.getCheckOutTime());

        // 4. WorkStatus 업데이트
        updateWorkStatusOnCheckOut(savedAttendance, checkOutDateTime);

        return savedAttendance;
    }

    /**
     * 퇴근 시 WorkStatus를 업데이트합니다.
     * 승인된 휴가(반차) 또는 조퇴 여부를 판단하고, 근무 시간을 계산하여 WorkDayType을 설정합니다.
     *
     * @param attendance 업데이트할 Attendance 엔티티
     * @param checkOutDateTime 퇴근 시각 (LocalDateTime)
     */
    private void updateWorkStatusOnCheckOut(Attendance attendance, LocalDateTime checkOutDateTime) {
        Optional<WorkStatus> optionalWorkStatus = workStatusRepository.findByAttendanceId(attendance.getId());

        if (optionalWorkStatus.isPresent()) {
            WorkStatus workStatus = optionalWorkStatus.get();
            workStatus.setCheckOutTime(checkOutDateTime.toLocalTime());

            LocalTime currentCheckOutTime = checkOutDateTime.toLocalTime();
            LocalTime standardCheckOutTime = LocalTime.parse(standardCheckOutTimeStr);

            // 1. 승인된 휴가 타입 확인 (오후 반차, 조퇴 등)
            String approvedLeaveType = approvalServiceClient.getApprovedLeaveType(attendance.getUserId(), checkOutDateTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            if (approvedLeaveType != null) {
                log.debug("User {} has approved leave on check-out: {}", attendance.getUserId(), approvedLeaveType);
                WorkStatusType leaveStatusType = getWorkStatusTypeFromApprovedLeaveType(approvedLeaveType);
                if (leaveStatusType != null) {
                    workStatus.setStatusType(leaveStatusType);
                } else {
                    // 알 수 없는 휴가 타입은 기존 상태 유지
                    log.warn("Unknown approved leave type for user {} on check-out: {}", attendance.getUserId(), approvedLeaveType);
                }
            } else if (currentCheckOutTime.isBefore(standardCheckOutTime)) {
                // 2. 승인된 휴가가 없고, 표준 퇴근 시간보다 일찍 퇴근하면 조퇴 처리
                log.debug("User {} is early leave. Check-out time: {}, Standard check-out time: {}", attendance.getUserId(), currentCheckOutTime, standardCheckOutTime);
                workStatus.setStatusType(WorkStatusType.EARLY_LEAVE);
            }

            // 3. 근무 시간 계산 및 WorkDayType 설정
                        Map<String, Duration> workTimes = WorkTimeCalculator.calculateWorkTimes(attendance.getCheckInTime(), checkOutDateTime, attendance.getAttendanceDate());
            Duration totalWorkDuration = workTimes.get("totalWorkTime");

            if (totalWorkDuration.toHours() >= 8) {
                workStatus.setWorkDayType(WorkDayType.FULL_DAY);
            } else if (totalWorkDuration.toHours() >= 4) {
                workStatus.setWorkDayType(WorkDayType.HALF_DAY);
            } else {
                workStatus.setWorkDayType(WorkDayType.NONE); // 4시간 미만 근무 시
            }

            workStatusRepository.save(workStatus);
            log.info("WorkStatus 업데이트 및 저장 완료. WorkStatus ID: {}, CheckOutTime: {}, WorkDayType: {}",
                    workStatus.getId(), workStatus.getCheckOutTime(), workStatus.getWorkDayType());
        } else {
            log.warn("Attendance ID {}에 해당하는 WorkStatus를 찾을 수 없습니다. WorkStatus 업데이트를 건너뜁니다.", attendance.getId());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AttendanceResDto> getMonthlyAttendances(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Attendance> monthlyAttendances = attendanceRepository.findByUserIdAndAttendanceDateBetween(userId, startDate, endDate);

        return monthlyAttendances.stream().map(attendance -> {
            if (attendance.getCheckOutTime() != null) {
                Map<String, Duration> workTimes = WorkTimeCalculator.calculateWorkTimes(attendance.getCheckInTime(), attendance.getCheckOutTime(), attendance.getAttendanceDate());
                return AttendanceResDto.from(attendance,
                        WorkTimeCalculator.formatDuration(workTimes.get("totalWorkTime")),
                        WorkTimeCalculator.formatDuration(workTimes.get("normalWorkTime")),
                        WorkTimeCalculator.formatDuration(workTimes.get("overtimeWorkTime")),
                        WorkTimeCalculator.formatDuration(workTimes.get("nightWorkTime"))
                );
            } else {
                return AttendanceResDto.from(attendance, null, null, null, null);
            }
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Attendance recordGoOut(Long userId) {
        LocalDate today = LocalDate.now();
        // 1. 오늘 출근 기록이 있는지 확인
        Attendance attendance = attendanceRepository.findByUserIdAndAttendanceDate(userId, today)
                .orElseThrow(() -> new CheckInNotFoundException("오늘 출근 기록이 존재하지 않아 외출할 수 없습니다. (User ID: " + userId + ")"));

        // 2. 이미 퇴근했는지 확인
        if (attendance.getCheckOutTime() != null) {
            throw new AlreadyCheckedOutException("이미 퇴근하여 외출할 수 없습니다.");
        }

        // 3. 이미 외출 중인지 확인
        if (attendance.getGoOutTime() != null) {
            throw new AlreadyOnLeaveException("이미 외출 중입니다.");
        }

        // 4. 외출 시간 기록
        attendance.recordGoOut();
        Attendance savedAttendance = attendanceRepository.save(attendance);
        log.info("외출 기록 완료. Attendance ID: {}, User ID: {}, GoOutTime: {}",
                savedAttendance.getId(), userId, savedAttendance.getGoOutTime());

        // 5. WorkStatus 업데이트 (지각이 아닌 경우에만 외출 상태로 변경)
        WorkStatus workStatus = savedAttendance.getWorkStatus();
        if (workStatus != null) {
            if (workStatus.getStatusType() != WorkStatusType.LATE) {
                workStatus.setStatusType(WorkStatusType.SHORT_LEAVE);
                workStatusRepository.save(workStatus);
                log.info("WorkStatus 업데이트 완료. WorkStatus ID: {}, StatusType: {}", workStatus.getId(), workStatus.getStatusType());
            }
        } else {
            log.warn("Attendance ID {}에 해당하는 WorkStatus를 찾을 수 없습니다. WorkStatus 업데이트를 건너뜁니다.", savedAttendance.getId());
        }

        return savedAttendance;
    }

    @Override
    @Transactional
    public Attendance recordReturn(Long userId) {
        LocalDate today = LocalDate.now();
        // 1. 오늘 출근 기록이 있는지 확인
        Attendance attendance = attendanceRepository.findByUserIdAndAttendanceDate(userId, today)
                .orElseThrow(() -> new CheckInNotFoundException("오늘 출근 기록이 존재하지 않아 복귀할 수 없습니다. (User ID: " + userId + ")"));

        // 2. 외출 기록이 있는지 확인
        if (attendance.getGoOutTime() == null) {
            throw new AlreadyOnLeaveException("외출 기록이 없어 복귀할 수 없습니다.");
        }

        // 3. 이미 복귀 처리되었는지 확인
        if (attendance.getReturnTime() != null) {
            throw new AlreadyOnLeaveException("이미 복귀 처리되었습니다.");
        }

        // 4. 복귀 시간 기록
        attendance.recordReturn();
        Attendance savedAttendance = attendanceRepository.save(attendance);
        log.info("복귀 기록 완료. Attendance ID: {}, User ID: {}, ReturnTime: {}",
                savedAttendance.getId(), userId, savedAttendance.getReturnTime());

        // 5. WorkStatus 업데이트 (지각이 아닌 경우에만 외출 상태 유지)
        WorkStatus workStatus = savedAttendance.getWorkStatus();
        if (workStatus != null) {
            if (workStatus.getStatusType() != WorkStatusType.LATE) {
                // 외출 후 복귀했으므로 WorkStatusType은 SHORT_LEAVE를 유지하거나, 필요에 따라 REGULAR로 변경 가능
                // 현재는 SHORT_LEAVE 유지 (외출 기록이 있었음을 나타냄)
                workStatusRepository.save(workStatus);
                log.info("WorkStatus 업데이트 완료. WorkStatus ID: {}, StatusType: {}", workStatus.getId(), workStatus.getStatusType());
            }
        } else {
            log.warn("Attendance ID {}에 해당하는 WorkStatus를 찾을 수 없습니다. WorkStatus 업데이트를 건너뜁니다.", savedAttendance.getId());
        }

        return savedAttendance;
    }

    @Override
    @Transactional(readOnly = true)
    public WorkTimeDto getRemainingWorkTime(Long userId) {
        LocalDate today = LocalDate.now();
        Optional<Attendance> optionalAttendance = attendanceRepository.findByUserIdAndAttendanceDate(userId, today);

        if (optionalAttendance.isEmpty()) {
            return new WorkTimeDto("08:00", "00:00");
        }

        Attendance attendance = optionalAttendance.get();
        LocalDateTime checkInTime = attendance.getCheckInTime();
        LocalDateTime now = LocalDateTime.now();

        Duration actualWorkedDuration;

        if (attendance.getCheckOutTime() != null) {
            // 퇴근 시간이 기록된 경우, 퇴근 시간까지의 실제 근무 시간 계산
            actualWorkedDuration = WorkTimeCalculator.calculateActualWorkedDuration(
                    checkInTime,
                    attendance.getCheckOutTime(),
                    attendance.getGoOutTime(),
                    attendance.getReturnTime()
            );
            String workedTime = WorkTimeCalculator.formatDuration(actualWorkedDuration);
            return new WorkTimeDto("00:00", workedTime); // 퇴근했으므로 남은 근무 시간은 0
        } else {
            // 퇴근 시간이 기록되지 않은 경우, 현재 시간까지의 실제 근무 시간 계산
            actualWorkedDuration = WorkTimeCalculator.calculateActualWorkedDuration(
                    checkInTime,
                    now,
                    attendance.getGoOutTime(),
                    attendance.getReturnTime()
            );
        }

        String workedTime = WorkTimeCalculator.formatDuration(actualWorkedDuration);

        long standardWorkMinutes = WorkConstants.STANDARD_WORK_HOURS * 60;
        long remainingMinutesRounded = standardWorkMinutes - actualWorkedDuration.toMinutes();

        if (remainingMinutesRounded < 0) {
            remainingMinutesRounded = 0;
        }

        String remainingTime = WorkTimeCalculator.formatDuration(Duration.ofMinutes(remainingMinutesRounded));

        return new WorkTimeDto(remainingTime, workedTime);
    }

    

    @Override
    @Transactional(readOnly = true)
    public Optional<AttendanceResDto> getTodayAttendance(Long userId) {
        LocalDate today = LocalDate.now();
        Optional<Attendance> optionalAttendance = attendanceRepository.findByUserIdAndAttendanceDate(userId, today);

        return optionalAttendance.map(attendance -> {
            if (attendance.getCheckOutTime() != null) {
                Map<String, Duration> workTimes = WorkTimeCalculator.calculateWorkTimes(attendance.getCheckInTime(), attendance.getCheckOutTime(), attendance.getAttendanceDate());
                return AttendanceResDto.from(attendance,
                        WorkTimeCalculator.formatDuration(workTimes.get("totalWorkTime")),
                        WorkTimeCalculator.formatDuration(workTimes.get("normalWorkTime")),
                        WorkTimeCalculator.formatDuration(workTimes.get("overtimeWorkTime")),
                        WorkTimeCalculator.formatDuration(workTimes.get("nightWorkTime"))
                );
            } else {
                return AttendanceResDto.from(attendance, null, null, null, null);
            }
        });
    }

    @Override
    public PersonalAttendanceStatsDto getPersonalAttendanceStats(Long userId, int year, int month) {
        List<Attendance> monthlyAttendances = attendanceRepository.findByUserIdAndAttendanceDateBetween(userId, YearMonth.of(year, month).atDay(1), YearMonth.of(year, month).atEndOfMonth());

        long attendanceCount = monthlyAttendances.stream()
                .filter(a -> a.getWorkStatus() != null)
                .count();
        long lateCount = monthlyAttendances.stream()
                .filter(a -> a.getWorkStatus() != null && a.getWorkStatus().isLate())
                .count();
        long goOutCount = monthlyAttendances.stream()
                .filter(a -> a.getGoOutTime() != null)
                .count();

        CommonResDto<MonthlyVacationStatsDto> vacationStatsResponse = vacationServiceClient.getMonthlyVacationStats(userId, year, month);
        MonthlyVacationStatsDto vacationStats = vacationStatsResponse.getResult();

        return PersonalAttendanceStatsDto.builder()
                .attendanceCount(attendanceCount)
                .lateCount(lateCount)
                .goOutCount(goOutCount)
                .fullDayVacationCount(vacationStats.getFullDayVacations())
                .halfDayVacationCount(vacationStats.getHalfDayVacations())
                .build();
    }

    @Override
    public CommonResDto<com.playdata.attendanceservice.client.dto.VacationBalanceResDto> getPersonalVacationBalance(Long userId) {
        return vacationServiceClient.getVacationBalance(userId);
    }

    @Override
    @Transactional
    public void requestHalfDayVacation(Long userId, com.playdata.attendanceservice.client.dto.VacationRequestDto requestDto) {
        requestDto.setUserId(userId);
        vacationServiceClient.requestVacation(requestDto);
    }

    @Override
    public CommonResDto<List<com.playdata.attendanceservice.client.dto.Vacation>> getPersonalMonthlyHalfDayVacations(Long userId, int year, int month) {
        return vacationServiceClient.getMonthlyHalfDayVacations(userId, year, month);
    }

    @Override
    public String getLateThreshold() {
        return standardCheckInTimeStr;
    }

    /**
     * 승인된 휴가 타입 문자열을 WorkStatusType으로 변환합니다.
     *
     * @param approvedLeaveType 승인된 휴가 타입 문자열 (예: "AM_HALF_DAY", "PM_HALF_DAY", "ANNUAL_LEAVE", "EARLY_LEAVE")
     * @return 해당 WorkStatusType 또는 null (알 수 없는 타입의 경우)
     */
    private WorkStatusType getWorkStatusTypeFromApprovedLeaveType(String approvedLeaveType) {
        if (approvedLeaveType == null) {
            return null;
        }
        switch (approvedLeaveType) {
            case "AM_HALF_DAY":
                return WorkStatusType.AM_HALF_DAY;
            case "PM_HALF_DAY":
                return WorkStatusType.PM_HALF_DAY;
            case "ANNUAL_LEAVE":
                return WorkStatusType.ANNUAL_LEAVE;
            case "EARLY_LEAVE":
                return WorkStatusType.EARLY_LEAVE;
            default:
                log.warn("Unknown approved leave type: {}", approvedLeaveType);
                return null;
        }
    }

    
}