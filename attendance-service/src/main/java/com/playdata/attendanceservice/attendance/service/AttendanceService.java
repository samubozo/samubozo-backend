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

/**
 * 근태 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 이 클래스는 주로 출근 기록, 퇴근 기록과 같은 핵심 비즈니스 기능을 담당합니다.
 *
 * @Service 어노테이션: 이 클래스가 Spring의 서비스 계층 컴포넌트임을 나타냅니다.
 *                   Spring이 이 클래스를 스캔하여 빈(Bean)으로 등록하고,
 *                   다른 컴포넌트에서 의존성 주입(Dependency Injection)을 통해 사용할 수 있도록 합니다.
 *
 * @RequiredArgsConstructor 어노테이션 (Lombok):
 *                          클래스 내의 'final'로 선언된 필드들을 인자로 받는 생성자를 자동으로 생성해줍니다.
 *                          여기서는 'attendanceRepository' 필드가 final이므로,
 *                          Spring이 이 생성자를 통해 'AttendanceRepository' 빈을 자동으로 주입해줍니다.
 *                          이를 '생성자 주입'이라고 하며, 의존성 주입의 가장 권장되는 방식입니다。
 *
 * @Transactional(readOnly = true) 어노테이션:
 *                                 클래스 레벨에 트랜잭션 설정을 적용합니다.
 *                                 기본적으로 이 클래스의 모든 메소드는 읽기 전용(readOnly = true) 트랜잭션으로 실행됩니다.
 *                                 이는 데이터를 변경하지 않는 조회 메소드에 적합합니다.
 *                                 데이터를 변경하는 메소드(예: checkIn, checkOut)에는 별도로 @Transactional을 적용하여
 *                                 쓰기 가능한 트랜잭션으로 오버라이드할 수 있습니다.
 *                                 트랜잭션은 데이터베이스 작업의 원자성(Atomicity), 일관성(Consistency),
 *                                 고립성(Isolation), 지속성(Durability)을 보장하는 중요한 개념입니다.
 */
@Service
@RequiredArgsConstructor
@Slf4j // Slf4j 로거 사용을 위한 어노테이션 추가
@Transactional(readOnly = true)
public class AttendanceService {

    // AttendanceRepository는 Attendance 엔티티에 대한 데이터베이스 접근을 담당하는 인터페이스입니다.
    // Spring이 이 필드에 AttendanceRepository의 구현체(프록시 객체)를 자동으로 주입해줍니다.
    private final AttendanceRepository attendanceRepository;
    private final WorkStatusRepository workStatusRepository; // WorkStatusRepository 주입
    private final HrServiceClient hrServiceClient; // HrServiceClient 주입
    private final VacationServiceClient vacationServiceClient; // VacationServiceClient 주입
    private final ApprovalServiceClient approvalServiceClient; // ApprovalServiceClient 주입

    @Value("${standard.checkin.time}")
    private String standardCheckInTimeStr;

    @Value("${standard.checkout.time}")
    private String standardCheckOutTimeStr;

    /**
     * 사용자의 출근을 기록하는 메소드입니다.
     * 이 메소드는 사용자가 하루에 한 번만 출근 기록을 할 수 있도록 제약 조건을 처리합니다.
     *
     * @param userId    출근을 기록할 사용자의 고유 ID
     * @param ipAddress 출근 기록 시 사용된 클라이언트의 IP 주소
     * @return 성공적으로 저장된 Attendance 엔티티 객체
     * @throws IllegalStateException: 만약 해당 사용자가 이미 오늘 출근 기록을 한 경우 (데이터베이스의 복합 유니크 제약 조건 위반 시)
     *                                이 예외를 발생시켜 클라이언트에게 이미 출근했음을 알립니다.
     * @Transactional 어노테이션:
     * 이 메소드는 데이터베이스에 새로운 출근 기록을 저장하는 쓰기 작업을 수행하므로,
     * 클래스 레벨의 readOnly = true 트랜잭션 설정을 오버라이드하여
     * 쓰기 가능한 트랜잭션으로 실행되도록 합니다.
     * 만약 이 메소드 실행 중 예외가 발생하면, 트랜잭션은 롤백되어 데이터베이스에 변경사항이 반영되지 않습니다.
     */
    @Transactional
    public Attendance recordCheckIn(Long userId, String ipAddress) {
        return recordCheckIn(userId, ipAddress, LocalDateTime.now());
    }

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
                }
            }

            WorkStatus workStatus = new WorkStatus(attendance, initialStatusType, null);
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


    /**
     * 사용자의 퇴근을 기록하는 메소드입니다.
     * 해당 사용자의 당일 출근 기록이 있어야만 퇴근 기록이 가능합니다.
     *
     * @param userId 퇴근을 기록할 사용자의 ID
     * @return 업데이트된 Attendance 엔티티
     * @throws IllegalArgumentException 당일 출근 기록이 없거나 이미 퇴근 기록이 된 경우 발생합니다.
     */
    @Transactional
    public Attendance recordCheckOut(Long userId) {
        return recordCheckOut(userId, LocalDateTime.now());
    }

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

    /**
     * 특정 사용자의 월별 근태 기록 목록을 조회합니다.
     *
     * @param userId 조회할 사용자의 ID
     * @param year   조회할 연도
     * @param month  조회할 월
     * @return 해당 월의 근태 기록 목록
     */
    @Transactional(readOnly = true)
    public List<Attendance> getMonthlyAttendances(Long userId, int year, int month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1); // 해당 월의 첫째 날
        LocalDate endDate = yearMonth.atEndOfMonth(); // 해당 월의 마지막 날

        // 리포지토리의 findByUserIdAndAttendanceDateBetween 메소드를 사용하여
        // 특정 사용자의 특정 기간(월)에 해당하는 근태 기록을 조회합니다.
        return attendanceRepository.findByUserIdAndAttendanceDateBetween(userId, startDate, endDate);
    }

    /**
     * 사용자의 외출을 기록하는 메소드입니다.
     *
     * @param userId 외출을 기록할 사용자의 ID
     * @return 업데이트된 Attendance 엔티티
     */
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
            // 현재 상태가 LATE가 아닌 경우에만 OUT_OF_OFFICE로 변경
            if (workStatus.getStatusType() != WorkStatusType.LATE) {
                workStatus.setStatusType(WorkStatusType.OUT_OF_OFFICE);
            }
            workStatusRepository.save(workStatus);
        }

        return attendanceRepository.save(attendance);
    }

    /**
     * 사용자의 복귀를 기록하는 메소드입니다.
     *
     * @param userId 복귀를 기록할 사용자의 ID
     * @return 업데이트된 Attendance 엔티티
     */
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
            workStatus.setStatusType(WorkStatusType.REGULAR); // 외출 후 복귀 시 정상 근무 상태로 변경
            workStatusRepository.save(workStatus);
        }

        return attendanceRepository.save(attendance);
    }

    /**
     * 사용자의 남은 근무 시간을 계산하여 반환합니다.
     * 하루 총 필요 근무 시간(예: 8시간)에서 현재까지 근무한 시간을 뺀 값을 계산합니다.
     * 퇴근하지 않았을 때만 계산하며, 퇴근했으면 "00:00"을 반환합니다.
     * 외출/복귀 시간을 고려하여 외출 중인 시간은 근무 시간에서 제외합니다.
     *
     * @param userId 남은 근무 시간을 조회할 사용자의 ID
     * @return "HH:mm" 형식의 남은 근무 시간 문자열
     */
    @Transactional(readOnly = true)
    public WorkTimeDto getRemainingWorkTime(Long userId) {
        LocalDate today = LocalDate.now();
        Optional<Attendance> optionalAttendance = attendanceRepository.findByUserIdAndAttendanceDate(userId, today);

        if (optionalAttendance.isEmpty()) {
            // 오늘 출근 기록이 없으면 남은 근무 시간은 8시간, 근무한 시간은 0시간
            return new WorkTimeDto("08:00", "00:00");
        }

        Attendance attendance = optionalAttendance.get();

        // 퇴근한 경우: 남은 시간은 0, 근무한 시간은 실제 근무시간
        if (attendance.getCheckOutTime() != null) {
            // 출근부터 퇴근까지의 실제 근무시간 계산
            LocalDateTime checkInTime = attendance.getCheckInTime();
            LocalDateTime checkOutTime = attendance.getCheckOutTime();
            Duration workedDuration = Duration.ZERO;

            if (attendance.getGoOutTime() != null) {
                LocalDateTime goOutTime = attendance.getGoOutTime();
                LocalDateTime returnTime = attendance.getReturnTime();

                if (returnTime != null) {
                    // 외출 시간을 제외한 실제 근무시간
                    workedDuration = Duration.between(checkInTime, goOutTime)
                            .plus(Duration.between(returnTime, checkOutTime));
                } else {
                    // 복귀하지 않은 경우
                    workedDuration = Duration.between(checkInTime, goOutTime);
                }
            } else {
                // 외출 기록이 없는 경우
                workedDuration = Duration.between(checkInTime, checkOutTime);
            }

            long workedHours = workedDuration.toHours();
            long workedMinutes = workedDuration.toMinutes() % 60;
            String workedTime = String.format("%02d:%02d", workedHours, workedMinutes);

            return new WorkTimeDto("00:00", workedTime);
        }

        // 2. 현재까지 근무한 시간 계산 (퇴근하지 않은 경우)
        LocalDateTime checkInTime = attendance.getCheckInTime();
        LocalDateTime now = LocalDateTime.now();

        // 출근 시간부터 현재까지의 총 경과 시간
        Duration totalElapsedDuration = Duration.between(checkInTime, now);

        // 외출 시간 계산
        Duration outingDuration = Duration.ZERO;
        if (attendance.getGoOutTime() != null) {
            LocalDateTime goOutTime = attendance.getGoOutTime();
            LocalDateTime returnTime = attendance.getReturnTime();

            if (returnTime != null) {
                // 외출 시작부터 복귀까지의 시간
                outingDuration = Duration.between(goOutTime, returnTime);
            } else {
                // 현재 외출 중인 경우: 외출 시작부터 현재까지의 시간
                outingDuration = Duration.between(goOutTime, now);
            }
        }

        // 실제 근무 시간 = 총 경과 시간 (외출 시간도 근무 시간에 포함)
        Duration actualWorkedDuration = totalElapsedDuration;

        // 근무 시간이 음수가 되는 경우 (예: 외출 시간이 너무 길어서) 0으로 처리
        if (actualWorkedDuration.isNegative()) {
            actualWorkedDuration = Duration.ZERO;
        }

        // 근무한 시간 계산 (분 단위로 올림)
        long workedTotalSeconds = actualWorkedDuration.getSeconds();
        long workedMinutesRounded = (workedTotalSeconds + 59) / 60; // 1초라도 있으면 다음 분으로 올림

        long workedHours = workedMinutesRounded / 60;
        long workedMinutes = workedMinutesRounded % 60;
        String workedTime = String.format("%02d:%02d", workedHours, workedMinutes);

        // 하루 총 필요 근무 시간 (8시간 = 480분)
        long standardWorkMinutes = 8 * 60;

        // 남은 근무 시간 계산 (분 단위로 올림된 근무 시간을 기준으로 계산)
        long remainingMinutesRounded = standardWorkMinutes - workedMinutesRounded;

        // 남은 시간이 음수이면 (초과 근무) 00:00으로 처리
        if (remainingMinutesRounded < 0) {
            remainingMinutesRounded = 0;
        }

        long remainingHours = remainingMinutesRounded / 60;
        long remainingMinutes = remainingMinutesRounded % 60;
        String remainingTime = String.format("%02d:%02d", remainingHours, remainingMinutes);

        return new WorkTimeDto(remainingTime, workedTime);
    }

    /**
     * 특정 사용자의 오늘 출근 기록을 조회합니다.
     *
     * @param userId 조회할 사용자의 ID
     * @return 오늘 출근 기록이 있다면 AttendanceResDto, 없다면 Optional.empty()
     */
    @Transactional(readOnly = true)
    public Optional<AttendanceResDto> getTodayAttendance(Long userId) {
        LocalDate today = LocalDate.now();
        Optional<Attendance> optionalAttendance = attendanceRepository.findByUserIdAndAttendanceDate(userId, today);

        return optionalAttendance.map(AttendanceResDto::from);
    }

    /**
     * 특정 사용자의 월별 근태 및 휴가 통계를 조회합니다.
     *
     * @param userId 사용자 ID
     * @param year   연도
     * @param month  월
     * @return 개인 근태 통계 DTO
     */
    public PersonalAttendanceStatsDto getPersonalAttendanceStats(Long userId, int year, int month) {
        // 1. 해당 월의 모든 근태 기록 조회
        List<Attendance> monthlyAttendances = getMonthlyAttendances(userId, year, month);

        // 2. 근태 기록 기반으로 통계 계산 (총 출근, 지각, 외출)
        long attendanceCount = monthlyAttendances.stream()
                .filter(a -> a.getWorkStatus() != null)
                .count();
        long lateCount = monthlyAttendances.stream()
                .filter(a -> a.getWorkStatus() != null && a.getWorkStatus().getStatusType() == WorkStatusType.LATE)
                .count();
        long goOutCount = monthlyAttendances.stream()
                .filter(a -> a.getGoOutTime() != null)
                .count();

        // 3. vacation-service에서 휴가 통계 조회
        CommonResDto<MonthlyVacationStatsDto> vacationStatsResponse = vacationServiceClient.getMonthlyVacationStats(userId, year, month);
        MonthlyVacationStatsDto vacationStats = vacationStatsResponse.getResult();

        // 4. 최종 통계 DTO 생성 및 반환
        return PersonalAttendanceStatsDto.builder()
                .attendanceCount(attendanceCount)
                .lateCount(lateCount)
                .goOutCount(goOutCount)
                .fullDayVacationCount(vacationStats.getFullDayVacations())
                .halfDayVacationCount(vacationStats.getHalfDayVacations())
                .build();
    }

    /**
     * 특정 사용자의 연차 현황을 조회합니다.
     *
     * @param userId 연차 현황을 조회할 사용자의 ID
     * @return 연차 현황 정보를 담은 VacationBalanceResDto
     */
    public CommonResDto<com.playdata.attendanceservice.client.dto.VacationBalanceResDto> getPersonalVacationBalance(Long userId) {
        return vacationServiceClient.getVacationBalance(userId);
    }

    /**
     * 반차를 신청합니다.
     *
     * @param userId 사용자 ID
     * @param requestDto 반차 신청 정보
     */
    @Transactional
    public void requestHalfDayVacation(Long userId, com.playdata.attendanceservice.client.dto.VacationRequestDto requestDto) {
        // VacationRequestDto에 userId 설정
        requestDto.setUserId(userId);
        vacationServiceClient.requestVacation(requestDto);
    }

    /**
     * 특정 사용자의 월별 반차 기록을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param year   조회할 연도
     * @param month  조회할 월
     * @return 월별 반차 기록 목록
     */
    public CommonResDto<List<com.playdata.attendanceservice.client.dto.Vacation>> getPersonalMonthlyHalfDayVacations(Long userId, int year, int month) {
        return vacationServiceClient.getMonthlyHalfDayVacations(userId, year, month);
    }

    /**
     * 지각 기준 시간을 조회합니다.
     *
     * @return 지각 기준 시간 문자열 (예: "09:00:00")
     */
    public String getLateThreshold() {
        return standardCheckInTimeStr;
    }
}
