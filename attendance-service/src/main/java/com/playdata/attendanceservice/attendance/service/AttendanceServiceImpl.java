package com.playdata.attendanceservice.attendance.service;

import com.playdata.attendanceservice.attendance.dto.AttendanceResDto;
import com.playdata.attendanceservice.attendance.dto.PersonalAttendanceStatsDto;
import com.playdata.attendanceservice.attendance.dto.WorkTimeDto;
import com.playdata.attendanceservice.attendance.entity.Attendance;
import com.playdata.attendanceservice.attendance.entity.WorkDayType;
import com.playdata.attendanceservice.attendance.entity.WorkStatus;
import com.playdata.attendanceservice.attendance.entity.WorkStatusType;
import com.playdata.attendanceservice.attendance.repository.AttendanceRepository;
import com.playdata.attendanceservice.attendance.repository.WorkStatusRepository;
import com.playdata.attendanceservice.client.ApprovalServiceClient;
import com.playdata.attendanceservice.client.HrServiceClient;
import com.playdata.attendanceservice.client.VacationServiceClient;
import com.playdata.attendanceservice.client.dto.MonthlyVacationStatsDto;
import com.playdata.attendanceservice.common.dto.CommonResDto;
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

    // 근무 시간 기준 상수 정의
    private static final LocalTime NORMAL_WORK_START = LocalTime.of(9, 0);
    private static final LocalTime NORMAL_WORK_END = LocalTime.of(18, 0);
    private static final LocalTime LUNCH_START = LocalTime.of(12, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(13, 0);
    private static final LocalTime OVERTIME_START = LocalTime.of(18, 0);
    private static final LocalTime OVERTIME_END = LocalTime.of(22, 0); // 연장 근무 종료 시간 추가
    private static final LocalTime NIGHT_WORK_START = LocalTime.of(22, 0);
    private static final LocalTime NIGHT_WORK_END = LocalTime.of(6, 0); // 익일 06:00

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
        try {
            Attendance attendance = Attendance.builder()
                    .userId(userId)
                    .attendanceDate(checkInDateTime.toLocalDate())
                    .ipAddress(ipAddress)
                    .build();

            attendance.updateCheckInTime(checkInDateTime);

            LocalTime currentCheckInTime = checkInDateTime.toLocalTime();
            LocalTime standardCheckInTime = LocalTime.parse(standardCheckInTimeStr);

            WorkStatusType initialStatusType = WorkStatusType.REGULAR;
            boolean isLate = false;

            String dateString = checkInDateTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String externalScheduleType = hrServiceClient.getApprovedExternalScheduleType(userId, dateString);
            if (externalScheduleType != null) {
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
                        initialStatusType = WorkStatusType.ABSENCE; // OUT_OF_OFFICE → ABSENCE로 변경
                        break;
                }
            } else {
                String approvedLeaveType = approvalServiceClient.getApprovedLeaveType(userId, checkInDateTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
                if (approvedLeaveType != null) {
                    switch (approvedLeaveType) {
                        case "AM_HALF_DAY":
                            initialStatusType = WorkStatusType.AM_HALF_DAY; // HALF_DAY_LEAVE → AM_HALF_DAY
                            break;
                        case "PM_HALF_DAY":
                            initialStatusType = WorkStatusType.PM_HALF_DAY; // 새로운 타입
                            break;
                        case "ANNUAL_LEAVE":
                            initialStatusType = WorkStatusType.ANNUAL_LEAVE; // 새로운 타입
                            break;
                        default:
                            break;
                    }
                } else if (currentCheckInTime.isAfter(standardCheckInTime)) {
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

            Attendance savedAttendance = attendanceRepository.save(attendance);
            log.info("Attendance saved after check-in: ID={}, CheckInTime={}, CheckOutTime={}", savedAttendance.getId(), savedAttendance.getCheckInTime(), savedAttendance.getCheckOutTime());

            workStatus.setCheckInTime(checkInDateTime.toLocalTime());
            workStatusRepository.save(workStatus);
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
        LocalDate today = checkOutDateTime.toLocalDate();
        Optional<Attendance> optionalAttendance = attendanceRepository.findByUserIdAndAttendanceDate(userId, today);

        Attendance attendance = optionalAttendance
                .orElseThrow(() -> new IllegalArgumentException("오늘 출근 기록이 존재하지 않아 퇴근할 수 없습니다. (User ID: " + userId + ")"));

        if (attendance.getCheckOutTime() != null) {
            throw new IllegalArgumentException("이미 퇴근 기록이 완료되었습니다. (User ID: " + userId + ")");
        }

        attendance.updateCheckOutTime(checkOutDateTime);
        log.info("Attendance will be saved with checkOutTime: {}", attendance.getCheckOutTime());

        Attendance savedAttendance = attendanceRepository.save(attendance);
        log.info("Attendance saved: ID={}, CheckInTime={}, CheckOutTime={}", savedAttendance.getId(), savedAttendance.getCheckInTime(), savedAttendance.getCheckOutTime());

        Optional<WorkStatus> optionalWorkStatus = workStatusRepository.findByAttendanceId(savedAttendance.getId());
        if (optionalWorkStatus.isPresent()) {
            WorkStatus workStatus = optionalWorkStatus.get();
            workStatus.setCheckOutTime(checkOutDateTime.toLocalTime());

            LocalTime currentCheckOutTime = checkOutDateTime.toLocalTime();
            LocalTime standardCheckOutTime = LocalTime.parse(standardCheckOutTimeStr);

            String approvedLeaveType = approvalServiceClient.getApprovedLeaveType(userId, checkOutDateTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE));
            if (approvedLeaveType != null) {
                switch (approvedLeaveType) {
                    case "PM_HALF_DAY":
                        workStatus.setStatusType(WorkStatusType.PM_HALF_DAY); // HALF_DAY_LEAVE → PM_HALF_DAY
                        break;
                    case "EARLY_LEAVE":
                        workStatus.setStatusType(WorkStatusType.EARLY_LEAVE);
                        break;
                    default:
                        break;
                }
            } else if (currentCheckOutTime.isBefore(standardCheckOutTime)) {
                workStatus.setStatusType(WorkStatusType.EARLY_LEAVE);
            }

            // 근무 시간 계산 로직 추가
            Map<String, Duration> workTimes = calculateWorkTimes(attendance.getCheckInTime(), checkOutDateTime, attendance.getAttendanceDate());
            Duration totalWorkDuration = workTimes.get("totalWorkTime");

            if (totalWorkDuration.toHours() >= 8) {
                workStatus.setWorkDayType(WorkDayType.FULL_DAY);
            } else if (totalWorkDuration.toHours() >= 4) {
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
    public List<AttendanceResDto> getMonthlyAttendances(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        List<Attendance> monthlyAttendances = attendanceRepository.findByUserIdAndAttendanceDateBetween(userId, startDate, endDate);

        return monthlyAttendances.stream().map(attendance -> {
            if (attendance.getCheckOutTime() != null) {
                Map<String, Duration> workTimes = calculateWorkTimes(attendance.getCheckInTime(), attendance.getCheckOutTime(), attendance.getAttendanceDate());
                return AttendanceResDto.from(attendance,
                        formatDuration(workTimes.get("totalWorkTime")),
                        formatDuration(workTimes.get("normalWorkTime")),
                        formatDuration(workTimes.get("overtimeWorkTime")),
                        formatDuration(workTimes.get("nightWorkTime"))
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
                workStatus.setStatusType(WorkStatusType.SHORT_LEAVE); // OUT_OF_OFFICE → SHORT_LEAVE로 변경
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
                workStatus.setStatusType(WorkStatusType.SHORT_LEAVE); // OUT_OF_OFFICE → SHORT_LEAVE로 변경
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

        return optionalAttendance.map(attendance -> {
            if (attendance.getCheckOutTime() != null) {
                Map<String, Duration> workTimes = calculateWorkTimes(attendance.getCheckInTime(), attendance.getCheckOutTime(), attendance.getAttendanceDate());
                return AttendanceResDto.from(attendance,
                        formatDuration(workTimes.get("totalWorkTime")),
                        formatDuration(workTimes.get("normalWorkTime")),
                        formatDuration(workTimes.get("overtimeWorkTime")),
                        formatDuration(workTimes.get("nightWorkTime"))
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
     * 출근 시간, 퇴근 시간, 근무 날짜를 기반으로 총 근무 시간, 정상 근무 시간, 연장 근무 시간, 심야 근무 시간을 계산합니다.
     * 점심 시간(12:00~13:00)은 근무 시간에서 제외됩니다.
     * 주말 근무는 전체 연장 근무로 간주됩니다.
     * 연장 근무는 18:00부터 22:00까지로 계산됩니다.
     * 심야 근무는 22:00부터 익일 06:00까지로 계산됩니다.
     * @param checkIn 출근 시간 (LocalDateTime)
     * @param checkOut 퇴근 시간 (LocalDateTime)
     * @param attendanceDate 근무 날짜 (LocalDate)
     * @return 각 근무 시간(totalWorkTime, normalWorkTime, overtimeWorkTime, nightWorkTime)을 포함하는 Map
     */
    private Map<String, Duration> calculateWorkTimes(LocalDateTime checkIn, LocalDateTime checkOut, LocalDate attendanceDate) {
        Map<String, Duration> workTimes = new HashMap<>();
        Duration totalWorkTime = Duration.ZERO;
        Duration normalWorkTime = Duration.ZERO;
        Duration overtimeWorkTime = Duration.ZERO;
        Duration nightWorkTime = Duration.ZERO;

        // 출근 시간이 퇴근 시간보다 늦거나 같으면 유효하지 않은 경우
        if (checkIn.isAfter(checkOut) || checkIn.isEqual(checkOut)) {
            workTimes.put("totalWorkTime", totalWorkTime);
            workTimes.put("normalWorkTime", normalWorkTime);
            workTimes.put("overtimeWorkTime", overtimeWorkTime);
            workTimes.put("nightWorkTime", nightWorkTime);
            return workTimes;
        }

        // 근무 시간 구간
        LocalDateTime workStart = checkIn;
        LocalDateTime workEnd = checkOut;

        // 점심 시간 구간 (오늘 날짜 기준)
        LocalDateTime lunchStartDateTime = attendanceDate.atTime(LUNCH_START);
        LocalDateTime lunchEndDateTime = attendanceDate.atTime(LUNCH_END);

        // 정상 근무 시간 구간 (오늘 날짜 기준)
        LocalDateTime normalWorkStartDateTime = attendanceDate.atTime(NORMAL_WORK_START);
        LocalDateTime normalWorkEndDateTime = attendanceDate.atTime(NORMAL_WORK_END);

        // 연장 근무 시간 구간 (오늘 날짜 기준)
        LocalDateTime overtimeStartDateTime = attendanceDate.atTime(OVERTIME_START);
        LocalDateTime overtimeEndDateTime = attendanceDate.atTime(OVERTIME_END);

        // 심야 근무 시간 구간 (오늘 날짜 기준 및 익일 기준)
        LocalDateTime nightWorkStartDateTimeToday = attendanceDate.atTime(NIGHT_WORK_START);
        LocalDateTime nightWorkEndDateTimeToday = attendanceDate.plusDays(1).atStartOfDay(); // 자정
        LocalDateTime nightWorkStartDateTimeNextDay = attendanceDate.plusDays(1).atStartOfDay(); // 자정
        LocalDateTime nightWorkEndDateTimeNextDay = attendanceDate.plusDays(1).atTime(NIGHT_WORK_END);

        // 주말(토, 일)인 경우 전체를 연장 근무로 처리
        if (attendanceDate.getDayOfWeek() == DayOfWeek.SATURDAY || attendanceDate.getDayOfWeek() == DayOfWeek.SUNDAY) {
            totalWorkTime = getOverlapDuration(workStart, workEnd, workStart, workEnd); // 전체 근무 시간
            totalWorkTime = totalWorkTime.minus(getOverlapDuration(workStart, workEnd, lunchStartDateTime, lunchEndDateTime)); // 점심시간 제외
            overtimeWorkTime = totalWorkTime; // 주말은 전체가 연장 근무
        } else {
            // 평일 근무 시간 계산
            // 총 근무 시간 = 전체 근무 시간 - 점심 시간
            totalWorkTime = getOverlapDuration(workStart, workEnd, workStart, workEnd);
            totalWorkTime = totalWorkTime.minus(getOverlapDuration(workStart, workEnd, lunchStartDateTime, lunchEndDateTime));

            // 정상 근무 시간
            // 실제 근무 시간과 정상 근무 시간 구간의 겹치는 부분을 먼저 계산
            Duration grossNormalWorkTime = getOverlapDuration(workStart, workEnd, normalWorkStartDateTime, normalWorkEndDateTime);
            // 실제 근무 시간과 점심 시간 구간의 겹치는 부분을 계산
            Duration lunchOverlapWithActualWork = getOverlapDuration(workStart, workEnd, lunchStartDateTime, lunchEndDateTime);
            // 총 정상 근무 시간에서 실제 근무 시간과 겹치는 점심 시간을 제외
            normalWorkTime = grossNormalWorkTime.minus(lunchOverlapWithActualWork);

            // 연장 근무 시간 (18:00 ~ 22:00)
            overtimeWorkTime = getOverlapDuration(workStart, workEnd, overtimeStartDateTime, overtimeEndDateTime);

            // 심야 근무 시간 (22:00 ~ 익일 06:00)
            nightWorkTime = nightWorkTime.plus(getOverlapDuration(workStart, workEnd, nightWorkStartDateTimeToday, nightWorkEndDateTimeToday)); // 22:00 ~ 자정
            nightWorkTime = nightWorkTime.plus(getOverlapDuration(workStart, workEnd, nightWorkStartDateTimeNextDay, nightWorkEndDateTimeNextDay)); // 자정 ~ 익일 06:00
        }

        workTimes.put("totalWorkTime", totalWorkTime);
        workTimes.put("normalWorkTime", normalWorkTime);
        workTimes.put("overtimeWorkTime", overtimeWorkTime);
        workTimes.put("nightWorkTime", nightWorkTime);
        return workTimes;
    }

    // 두 시간 구간의 겹치는 Duration을 반환하는 헬퍼 메서드
    private Duration getOverlapDuration(LocalDateTime start1, LocalDateTime end1, LocalDateTime start2, LocalDateTime end2) {
        LocalDateTime overlapStart = max(start1, start2);
        LocalDateTime overlapEnd = min(end1, end2);

        if (overlapStart.isBefore(overlapEnd)) {
            return Duration.between(overlapStart, overlapEnd);
        }
        return Duration.ZERO;
    }

    // LocalDateTime.max 및 min 헬퍼 (Java 8 호환성을 위해 직접 구현)
    private LocalDateTime max(LocalDateTime a, LocalDateTime b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalDateTime min(LocalDateTime a, LocalDateTime b) {
        return a.isBefore(b) ? a : b;
    }

    // Duration을 "HH:mm" 형식의 String으로 포맷하는 헬퍼 메서드
    private String formatDuration(Duration duration) {
        if (duration == null) {
            return null;
        }
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        return String.format("%02d:%02d", hours, minutes);
    }
}