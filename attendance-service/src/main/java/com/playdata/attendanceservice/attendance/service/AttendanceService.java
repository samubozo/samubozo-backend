package com.playdata.attendanceservice.attendance.service;

import com.playdata.attendanceservice.attendance.entity.Attendance;
import com.playdata.attendanceservice.attendance.entity.WorkStatus;
import com.playdata.attendanceservice.attendance.entity.WorkStatusType;
import com.playdata.attendanceservice.attendance.repository.AttendanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException; // DataIntegrityViolationException 임포트
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Transactional(readOnly = true)
public class AttendanceService {

    // AttendanceRepository는 Attendance 엔티티에 대한 데이터베이스 접근을 담당하는 인터페이스입니다.
    // Spring이 이 필드에 AttendanceRepository의 구현체(프록시 객체)를 자동으로 주입해줍니다.
    private final AttendanceRepository attendanceRepository;

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
        // LocalDate today = LocalDate.now(); // 기존 코드에서 사용되던 변수, 이제 필요 없음

        try {
            // 1. Attendance 엔티티 객체 생성
            //    Builder 패턴을 사용하여 Attendance 객체를 생성합니다.
            //    Lombok의 @Builder 어노테이션 덕분에 Attendance.builder().필드(값).build() 형태로
            //    객체를 쉽게 생성할 수 있습니다.
            // 1. Attendance 엔티티 객체 생성
            //    Builder 패턴을 사용하여 Attendance 객체를 생성합니다.
            //    Lombok의 @Builder 어노테이션 덕분에 Attendance.builder().필드(값).build() 형태로
            //    객체를 쉽게 생성할 수 있습니다.
            Attendance attendance = Attendance.builder()
                    .userId(userId) // 출근하는 사용자의 ID 설정
                    .attendanceDate(LocalDate.now()) // 현재 날짜를 출근 날짜로 설정 (예: 2025-07-04)
                    .ipAddress(ipAddress) // 출근 시 사용된 IP 주소 설정
                    // checkInTime 필드는 Attendance 엔티티의 @CreationTimestamp 어노테이션에 의해
                    // 데이터베이스에 저장될 때 자동으로 현재 시간으로 설정됩니다.
                    .build();

            // 2. WorkStatus 객체 생성 및 Attendance와 연결
            //    초기 출근 상태를 REGULAR로 설정합니다.
            WorkStatus workStatus = new WorkStatus(userId, LocalDate.now(), WorkStatusType.REGULAR, null);
            attendance.setWorkStatus(workStatus);

            // 2. Attendance 엔티티를 데이터베이스에 저장
            //    attendanceRepository.save(attendance) 메소드를 호출하여
            //    새로 생성된 Attendance 객체를 데이터베이스의 'attendance' 테이블에 삽입합니다.
            //    JPA는 이 메소드를 통해 객체를 SQL INSERT 문으로 변환하여 실행합니다.
            //    저장 후에는 데이터베이스에서 할당된 'id' 값이 attendance 객체에 다시 채워집니다.
            return attendanceRepository.save(attendance);

        } catch (DataIntegrityViolationException e) {
            // 3. 예외 처리: DataIntegrityViolationException 발생 시
            //    이 예외는 데이터베이스의 무결성 제약 조건(Integrity Constraint)을 위반했을 때 발생합니다.
            //    우리의 경우, Attendance 엔티티에 (userId, attendanceDate) 복합 유니크 제약 조건이 있으므로,
            //    만약 동일한 userId와 attendanceDate를 가진 기록이 이미 데이터베이스에 존재하는데
            //    또 다시 저장하려고 할 때 이 예외가 발생합니다.
            //    이는 "한 사용자는 하루에 한 번만 출근할 수 있다"는 비즈니스 규칙을 위반한 경우입니다.

            // 사용자에게 더 명확한 메시지를 전달하기 위해 IllegalStateException을 발생시킵니다.
            // 이 예외는 컨트롤러 계층에서 적절히 처리되어 사용자에게 "이미 출근했습니다"와 같은 메시지를 보여줄 수 있습니다.
            throw new IllegalStateException("이미 오늘 출근 기록이 존재합니다. (User ID: " + userId + ", Date: " + LocalDate.now() + ")", e);
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
        // 1. 해당 사용자의 당일 출근 기록을 찾습니다.
        //    리포지토리의 findByUserIdAndAttendanceDate 메소드를 사용하여 조회합니다.
        //    Optional<Attendance>는 결과가 있을 수도 있고 없을 수도 있음을 나타냅니다.
        Optional<Attendance> optionalAttendance = attendanceRepository.findByUserIdAndAttendanceDate(userId, today);

        // 2. 출근 기록이 없는 경우 예외 발생
        //    orElseThrow()를 사용하여 Optional이 비어있으면 즉시 예외를 발생시킵니다.
        Attendance attendance = optionalAttendance
                .orElseThrow(() -> new IllegalArgumentException("오늘 출근 기록이 존재하지 않아 퇴근할 수 없습니다. (User ID: " + userId + ")"));

        // 3. 이미 퇴근 기록이 되어 있는지 확인합니다.
        if (attendance.getCheckOutTime() != null) {
            throw new IllegalArgumentException("이미 퇴근 기록이 완료되었습니다. (User ID: " + userId + ")");
        }

        // 4. 퇴근 시간을 설정하고 업데이트합니다.
        //    Attendance 엔티티의 checkOutTime 필드에 @UpdateTimestamp 어노테이션이 있으므로,
        //    엔티티가 업데이트될 때 자동으로 현재 시간이 기록됩니다.
        //    따라서 명시적으로 setCheckOutTime()을 호출할 필요가 없습니다.

        // 5. 변경된 엔티티를 데이터베이스에 저장
        //    JPA의 변경 감지(Dirty Checking) 기능 덕분에 @Transactional 메소드 내에서는
        //    엔티티의 상태가 변경되면 save()를 명시적으로 호출하지 않아도 자동으로 업데이트됩니다.
        //    하지만 명확성을 위해 save()를 호출하는 것도 좋은 습관입니다.
        return attendanceRepository.save(attendance);
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
}