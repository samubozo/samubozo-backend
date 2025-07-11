package com.playdata.attendanceservice.attendance.service;

import com.playdata.attendanceservice.attendance.entity.Attendance;
import com.playdata.attendanceservice.attendance.entity.WorkDayType;
import com.playdata.attendanceservice.attendance.entity.WorkStatus;
import com.playdata.attendanceservice.attendance.entity.WorkStatusType;
import com.playdata.attendanceservice.attendance.repository.AttendanceRepository;
import com.playdata.attendanceservice.attendance.repository.WorkStatusRepository; // WorkStatusRepository 임포트 추가
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException; // DataIntegrityViolationException 임포트
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime; // LocalDateTime 임포트
import java.time.YearMonth;
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
 *                          이를 '생성자 주입'이라고 하며, 의존성 주입의 가장 권장되는 방식입니다.
 *
 * @Transactional(readOnly = true) 어노테이션:
 *                                 클래스 레벨에 트랜잭션 설정을 적용합니다。
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

    /**
     * 사용자의 출근을 기록하는 메소드입니다.
     * 이 메소드는 사용자가 하루에 한 번만 출근 기록을 할 수 있도록 제약 조건을 처리합니다.
     *
     * @param userId 출근을 기록할 사용자의 고유 ID
     * @param ipAddress 출근 기록 시 사용된 클라이언트의 IP 주소
     * @return 성공적으로 저장된 Attendance 엔티티 객체
     *
     * @Transactional 어노테이션:
     *                 이 메소드는 데이터베이스에 새로운 출근 기록을 저장하는 쓰기 작업을 수행하므로,
     *                 클래스 레벨의 readOnly = true 트랜잭션 설정을 오버라이드하여
     *                 쓰기 가능한 트랜잭션으로 실행되도록 합니다.
     *                 만약 이 메소드 실행 중 예외가 발생하면, 트랜잭션은 롤백되어 데이터베이스에 변경사항이 반영되지 않습니다.
     *
     * @throws IllegalStateException:
     *         만약 해당 사용자가 이미 오늘 출근 기록을 한 경우 (데이터베이스의 복합 유니크 제약 조건 위반 시)
     *         이 예외를 발생시켜 클라이언트에게 이미 출근했음을 알립니다.
     */
    @Transactional
    public Attendance recordCheckIn(Long userId, String ipAddress) {
        try {
            Attendance attendance = Attendance.builder()
                    .userId(userId)
                    .attendanceDate(LocalDate.now())
                    .ipAddress(ipAddress)
                    // checkInTime은 @CreationTimestamp에 의해 자동 설정됩니다.
                    // checkOutTime은 명시적으로 설정하지 않으므로 초기에는 null이 됩니다.
                    .build();

            WorkStatus workStatus = new WorkStatus(attendance, WorkStatusType.REGULAR, null);
            attendance.setWorkStatus(workStatus);

            // Attendance와 WorkStatus를 함께 저장 (cascade 설정에 따라)
            Attendance savedAttendance = attendanceRepository.save(attendance);
            log.info("Attendance saved after check-in: ID={}, CheckInTime={}, CheckOutTime={}", savedAttendance.getId(), savedAttendance.getCheckInTime(), savedAttendance.getCheckOutTime());

            // WorkStatus의 checkInTime을 Attendance의 실제 checkInTime으로 업데이트
            // WorkStatus.java 엔티티의 checkInTime에는 @CreationTimestamp가 없으므로 수동 설정 필요
            workStatus.setCheckInTime(savedAttendance.getCheckInTime().toLocalTime());
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
        LocalDate today = LocalDate.now();
        Optional<Attendance> optionalAttendance = attendanceRepository.findByUserIdAndAttendanceDate(userId, today);

        Attendance attendance = optionalAttendance
                .orElseThrow(() -> new IllegalArgumentException("오늘 출근 기록이 존재하지 않아 퇴근할 수 없습니다. (User ID: " + userId + ")"));

        if (attendance.getCheckOutTime() != null) {
            throw new IllegalArgumentException("이미 퇴근 기록이 완료되었습니다. (User ID: " + userId + ")");
        }

        // ★★★ 이 부분이 중요합니다. Attendance 엔티티의 checkOutTime을 현재 시간으로 명시적으로 설정 ★★★
        LocalDateTime now = LocalDateTime.now();
        attendance.updateCheckOutTime(now); // Attendance 엔티티에 이 메서드가 있어야 합니다.
        log.info("Attendance will be saved with checkOutTime: {}", attendance.getCheckOutTime());

        Attendance savedAttendance = attendanceRepository.save(attendance); // 변경된 Attendance 저장
        log.info("Attendance saved: ID={}, CheckInTime={}, CheckOutTime={}", savedAttendance.getId(), savedAttendance.getCheckInTime(), savedAttendance.getCheckOutTime());

        // ★★★ WorkStatus의 checkOutTime도 현재 시간으로 명시적으로 설정 ★★★
        Optional<WorkStatus> optionalWorkStatus = workStatusRepository.findByAttendanceId(savedAttendance.getId());
        if (optionalWorkStatus.isPresent()) {
            WorkStatus workStatus = optionalWorkStatus.get();
            workStatus.setCheckOutTime(now.toLocalTime()); // LocalDateTime -> LocalTime 변환

            // 근무 시간 계산 로직 추가
            LocalDateTime checkInTime = attendance.getCheckInTime();
            LocalDateTime checkOutTime = now;
            Duration workDuration = Duration.between(checkInTime, checkOutTime);
            long workHours = workDuration.toHours();

            if (workHours >= 8) {
                workStatus.setWorkDayType(WorkDayType.FULL_DAY);
            } else if (workHours >= 4) {
                workStatus.setWorkDayType(WorkDayType.HALF_DAY);
            }

            workStatusRepository.save(workStatus); // 변경된 WorkStatus 저장
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
     * @param year 조회할 연도
     * @param month 조회할 월
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
            workStatus.setStatusType(WorkStatusType.OUT_OF_OFFICE);
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
}