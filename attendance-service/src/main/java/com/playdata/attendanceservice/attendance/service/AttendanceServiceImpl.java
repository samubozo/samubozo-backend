package com.playdata.attendanceservice.attendance.service;

import com.playdata.attendanceservice.attendance.dto.AttendanceResDto; // 추가
import com.playdata.attendanceservice.attendance.dto.PersonalAttendanceStatsDto;
import com.playdata.attendanceservice.attendance.dto.WorkTimeDto;
import com.playdata.attendanceservice.attendance.entity.Attendance;
import com.playdata.attendanceservice.attendance.entity.WorkDayType;
import com.playdata.attendanceservice.attendance.entity.WorkStatus;
import com.playdata.attendanceservice.attendance.entity.WorkStatusType;
import com.playdata.attendanceservice.attendance.repository.AttendanceRepository;
import com.playdata.attendanceservice.attendance.repository.WorkStatusRepository; // WorkStatusRepository 임포트 추가
import com.playdata.attendanceservice.client.ApprovalServiceClient; // ApprovalServiceClient 임포트
import com.playdata.attendanceservice.client.HrServiceClient; // HrServiceClient 임포트
import com.playdata.attendanceservice.client.VacationServiceClient; // VacationServiceClient 임포트
import com.playdata.attendanceservice.client.dto.MonthlyVacationStatsDto;
import com.playdata.attendanceservice.common.dto.CommonResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value; // Value 임포트
import org.springframework.dao.DataIntegrityViolationException; // DataIntegrityViolationException 임포트
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*; // LocalDateTime 임포트
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j // Slf4j 로거 사용을 위한 어노테이션 추가
@Transactional(readOnly = true)
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final WorkStatusRepository workStatusRepository; // WorkStatusRepository 주입
    private final HrServiceClient hrServiceClient; // HrServiceClient 주입
    private final VacationServiceClient vacationServiceClient; // VacationServiceClient 주입
    private final ApprovalServiceClient approvalServiceClient; // ApprovalServiceClient 주입

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
        try {
            Attendance attendance = Attendance.builder()
                    .userId(userId)
                    .attendanceDate(checkInDateTime.toLocalDate()) // checkInDateTime의 날짜 사용
                    .ipAddress(ipAddress)
                    .build();

            attendance.updateCheckInTime(checkInDateTime); // 파라미터로 받은 시간으로 설정

            LocalTime currentCheckInTime = checkInDateTime.toLocalTime();
            LocalTime standardCheckInTime = LocalTime.parse(standardCheckInTimeStr);

            WorkStatusType initialStatusType = WorkStatusType.REGULAR;
            boolean isLate = false;

            // 1. HR 서비스에서 승인된 외부 일정(출장, 연수 등)이 있는지 확인
            String dateString = checkInDateTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String externalScheduleType = hrServiceClient.getApprovedExternalScheduleType(userId, dateString);
            if (externalScheduleType != null) {
                // 외부 일정 유형에 따라 WorkStatusType 매핑
                switch (externalScheduleType) {
                    case "BUSINESS_TRIP":
                        initialStatusType = WorkStatusType.BUSINESS_TRIP;
                        break;
                    case "TRAINING":
                        initialStatusType = WorkStatusType.TRAINING;
                        break;
                    // TODO: 다른 외부 일정 유형에 대한 매핑 추가
                    default:
                        initialStatusType = WorkStatusType.OUT_OF_OFFICE; // 기본값
                        break;
                }
            } else {
                // 2. Approval 서비스에서 승인된 휴가(오전 반차)가 있는지 확인
                String approvedLeaveType = approvalServiceClient.getApprovedLeaveType(userId, checkInDateTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
                if (approvedLeaveType != null && approvedLeaveType.equals("HALF_DAY_LEAVE_AM")) { // 예시: 오전 반차 유형
                    initialStatusType = WorkStatusType.HALF_DAY_LEAVE;
                } else if (currentCheckInTime.isAfter(standardCheckInTime)) {
                    // 3. 승인된 외부 일정이나 오전 반차가 없고, 기준 시간보다 늦게 출근했으면 지각
                    initialStatusType = WorkStatusType.LATE;
                    isLate = true;
                }
            }

            WorkStatus workStatus = WorkStatus.builder()
                    .userId(userId)
                    .date(checkInDateTime.toLocalDate())
                    .statusType(initialStatusType)
                    .reason(null)
                    .checkInTime(checkInDateTime.toLocalTime())
                    .isLate(isLate)
                    .build();

            attendance.setWorkStatus(workStatus);

            // Attendance와 WorkStatus를 함께 저장 (cascade 설정에 따라)
            Attendance savedAttendance = attendanceRepository.save(attendance);
            log.info("Attendance saved after check-in: ID={}, CheckInTime={}, CheckOutTime={}", savedAttendance.getId(), savedAttendance.getCheckInTime(), savedAttendance.getCheckOutTime());

            // WorkStatus의 checkInTime을 Attendance의 실제 checkInTime으로 업데이트
            workStatus.setCheckInTime(checkInDateTime.toLocalTime()); // 파라미터로 받은 시간으로 설정
            workStatusRepository.save(workStatus); // WorkStatus를 명시적으로 저장
            log.info("WorkStatus after check-in: ID={}, CheckInTime={}", workStatus.getId(), workStatus.getCheckInTime());

            return savedAttendance;

        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("이미 오늘 출근 기록이 존재합니다. (User ID: " + userId + ", Date: " + LocalDate.now() + ")", e);
        } catch (Exception e) {
            log.error("출근 기록 중 예상치 못한 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("출근 기록 중 예상치 못한 오류가 발생했습니다.", e);
        }
    }


    @Override
    @Transactional
    public Attendance recordCheckOut(Long userId) {
        return recordCheckOut(userId, LocalDateTime.now());
    }

    @Override
    @Transactional
    public Attendance recordCheckOut(Long userId, LocalDateTime checkOutDateTime) {
        LocalDate today = checkOutDateTime.toLocalDate(); // checkOutDateTime의 날짜 사용
        Optional<Attendance> optionalAttendance = attendanceRepository.findByUserIdAndAttendanceDate(userId, today);

        Attendance attendance = optionalAttendance
                .orElseThrow(() -> new IllegalArgumentException("오늘 출근 기록이 존재하지 않아 퇴근할 수 없습니다. (User ID: " + userId + ")"));

        if (attendance.getCheckOutTime() != null) {
            throw new IllegalArgumentException("이미 퇴근 기록이 완료되었습니다. (User ID: " + userId + ")");
        }

        attendance.updateCheckOutTime(checkOutDateTime); // 파라미터로 받은 시간으로 설정
        log.info("Attendance will be saved with checkOutTime: {}", attendance.getCheckOutTime());

        Attendance savedAttendance = attendanceRepository.save(attendance);
        log.info("Attendance saved: ID={}, CheckInTime={}, CheckOutTime={}", savedAttendance.getId(), savedAttendance.getCheckInTime(), savedAttendance.getCheckOutTime());

        Optional<WorkStatus> optionalWorkStatus = workStatusRepository.findByAttendanceId(savedAttendance.getId());
        if (optionalWorkStatus.isPresent()) {
            WorkStatus workStatus = optionalWorkStatus.get();
            workStatus.setCheckOutTime(checkOutDateTime.toLocalTime()); // 파라미터로 받은 시간으로 설정

            LocalTime currentCheckOutTime = checkOutDateTime.toLocalTime();
            LocalTime standardCheckOutTime = LocalTime.parse(standardCheckOutTimeStr);

            // 1. Approval 서비스에서 승인된 부재(오후 반차, 조퇴)가 있는지 확인
            String approvedLeaveType = approvalServiceClient.getApprovedLeaveType(userId, checkOutDateTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            if (approvedLeaveType != null) {
                switch (approvedLeaveType) {
                    case "HALF_DAY_LEAVE_PM":
                        workStatus.setStatusType(WorkStatusType.HALF_DAY_LEAVE);
                        break;
                    case "EARLY_LEAVE":
                        workStatus.setStatusType(WorkStatusType.EARLY_LEAVE);
                        break;
                    // TODO: 다른 휴가 유형에 대한 매핑 추가
                    default:
                        // 승인된 휴가 유형이 있지만, 특별히 처리할 WorkStatusType이 없는 경우
                        break;
                }
            } else if (currentCheckOutTime.isBefore(standardCheckOutTime)) {
                // 2. 승인된 부재가 없고, 기준 시간보다 일찍 퇴근했으면 조퇴
                workStatus.setStatusType(WorkStatusType.EARLY_LEAVE);
            }

            // 근무 시간 계산 로직 추가
            LocalDateTime checkInTime = attendance.getCheckInTime();
            LocalDateTime checkOutTime = checkOutDateTime; // 파라미터로 받은 시간 사용
            Duration workDuration = Duration.between(checkInTime, checkOutTime);
            long workHours = workDuration.toHours();

            if (workHours >= 8) {
                workStatus.setWorkDayType(WorkDayType.FULL_DAY);
            } else if (workHours >= 4) {
                workStatus.setWorkDayType(WorkDayType.HALF_DAY);
            }

            workStatusRepository.save(workStatus);
            log.info("WorkStatus updated and saved: ID={}, CheckOutTime={}, WorkDayType={}", workStatus.getId(), workStatus.getCheckOutTime(), workStatus.getWorkDayType());
        } else {
            log.warn("No WorkStatus found for attendance ID: {}", savedAttendance.getId());
        }

        return savedAttendance;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Attendance> getMonthlyAttendances(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1); // 해당 월의 첫째 날
        LocalDate endDate = yearMonth.atEndOfMonth(); // 해당 월의 마지막 날

        return attendanceRepository.findByUserIdAndAttendanceDateBetween(userId, startDate, endDate);
    }

    @Override
    @Transactional
    public Attendance recordGoOut(Long userId) {
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByUserIdAndAttendanceDate(userId, today)
                .orElseThrow(() -> new IllegalArgumentException("오늘 출근 기록이 존재하지 않아 외출할 수 없습니다. (User ID: " + userId + ")"));

        if (attendance.getCheckOutTime() != null) {
            throw new IllegalStateException("이미 퇴근하여 외출할 수 없습니다.");
        }

        if (attendance.getGoOutTime() != null) {
            throw new IllegalStateException("이미 외출 중입니다.");
        }

        attendance.recordGoOut();

        WorkStatus workStatus = attendance.getWorkStatus();
        if (workStatus != null) {
            if (workStatus.getStatusType() != WorkStatusType.LATE) {
                workStatus.setStatusType(WorkStatusType.OUT_OF_OFFICE);
            }
            workStatusRepository.save(workStatus);
        }

        return attendanceRepository.save(attendance);
    }

    @Override
    @Transactional
    public Attendance recordReturn(Long userId) {
        LocalDate today = LocalDate.now();
        Attendance attendance = attendanceRepository.findByUserIdAndAttendanceDate(userId, today)
                .orElseThrow(() -> new IllegalArgumentException("오늘 출근 기록이 존재하지 않아 복귀할 수 없습니다. (User ID: " + userId + ")"));

        if (attendance.getGoOutTime() == null) {
            throw new IllegalStateException("외출 기록이 없어 복귀할 수 없습니다.");
        }

        if (attendance.getReturnTime() != null) {
            throw new IllegalStateException("이미 복귀 처리되었습니다.");
        }

        attendance.recordReturn();

        WorkStatus workStatus = attendance.getWorkStatus();
        if (workStatus != null) {
            if (workStatus.getStatusType() != WorkStatusType.LATE) {
                workStatus.setStatusType(WorkStatusType.OUT_OF_OFFICE);
            }
            workStatusRepository.save(workStatus);
        }

        return attendanceRepository.save(attendance);
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

        if (attendance.getCheckOutTime() != null) {
            LocalDateTime checkInTime = attendance.getCheckInTime();
            LocalDateTime checkOutTime = attendance.getCheckOutTime();
            Duration workedDuration = Duration.ZERO;

            if (attendance.getGoOutTime() != null) {
                LocalDateTime goOutTime = attendance.getGoOutTime();
                LocalDateTime returnTime = attendance.getReturnTime();

                if (returnTime != null) {
                    workedDuration = Duration.between(checkInTime, goOutTime)
                            .plus(Duration.between(returnTime, checkOutTime));
                } else {
                    workedDuration = Duration.between(checkInTime, goOutTime);
                }
            } else {
                workedDuration = Duration.between(checkInTime, checkOutTime);
            }

            long workedHours = workedDuration.toHours();
            long workedMinutes = workedDuration.toMinutes() % 60;
            String workedTime = String.format("%02d:%02d", workedHours, workedMinutes);

            return new WorkTimeDto("00:00", workedTime);
        }

        LocalDateTime checkInTime = attendance.getCheckInTime();
        LocalDateTime now = LocalDateTime.now();

        Duration totalElapsedDuration = Duration.between(checkInTime, now);

        Duration outingDuration = Duration.ZERO;
        if (attendance.getGoOutTime() != null) {
            LocalDateTime goOutTime = attendance.getGoOutTime();
            LocalDateTime returnTime = attendance.getReturnTime();

            if (returnTime != null) {
                outingDuration = Duration.between(goOutTime, returnTime);
            } else {
                outingDuration = Duration.between(goOutTime, now);
            }
        }

        Duration actualWorkedDuration = totalElapsedDuration;

        if (actualWorkedDuration.isNegative()) {
            actualWorkedDuration = Duration.ZERO;
        }

        long workedTotalSeconds = actualWorkedDuration.getSeconds();
        long workedMinutesRounded = (workedTotalSeconds + 59) / 60;

        long workedHours = workedMinutesRounded / 60;
        long workedMinutes = workedMinutesRounded % 60;
        String workedTime = String.format("%02d:%02d", workedHours, workedMinutes);

        long standardWorkMinutes = 8 * 60;

        long remainingMinutesRounded = standardWorkMinutes - workedMinutesRounded;

        if (remainingMinutesRounded < 0) {
            remainingMinutesRounded = 0;
        }

        long remainingHours = remainingMinutesRounded / 60;
        long remainingMinutes = remainingMinutesRounded % 60;
        String remainingTime = String.format("%02d:%02d", remainingHours, remainingMinutes);

        return new WorkTimeDto(remainingTime, workedTime);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AttendanceResDto> getTodayAttendance(Long userId) {
        LocalDate today = LocalDate.now();
        Optional<Attendance> optionalAttendance = attendanceRepository.findByUserIdAndAttendanceDate(userId, today);

        return optionalAttendance.map(AttendanceResDto::from);
    }

    @Override
    public PersonalAttendanceStatsDto getPersonalAttendanceStats(Long userId, int year, int month) {
        List<Attendance> monthlyAttendances = getMonthlyAttendances(userId, year, month);

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
}
