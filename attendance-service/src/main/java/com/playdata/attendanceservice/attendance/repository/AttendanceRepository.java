package com.playdata.attendanceservice.attendance.repository;

import com.playdata.attendanceservice.attendance.entity.Attendance;
import com.playdata.attendanceservice.attendance.entity.WorkStatus;
import com.playdata.attendanceservice.attendance.entity.WorkStatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Attendance 엔티티의 데이터베이스 접근을 위한 리포지토리 인터페이스입니다.
 * Spring Data JPA의 JpaRepository를 상속받아 기본적인 CRUD 기능을 제공받습니다.
 */
@Repository // 이 인터페이스가 Spring의 리포지토리 컴포넌트임을 나타냅니다.
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    /**
     * 특정 사용자의 특정 날짜에 해당하는 근태 기록을 조회합니다.
     *
     * @param userId 조회할 사용자의 ID
     * @param attendanceDate 조회할 근태 기록의 날짜
     * @return 해당 조건에 맞는 Attendance 엔티티 (존재하지 않을 경우 Optional.empty())
     */
    Optional<Attendance> findByUserIdAndAttendanceDate(Long userId, LocalDate attendanceDate);

    /**
     * 특정 사용자의 특정 기간 내 근태 기록 목록을 조회합니다.
     *
     * @param userId 조회할 사용자의 ID
     * @param startDate 조회 시작 날짜
     * @param endDate 조회 종료 날짜
     * @return 해당 조건에 맞는 Attendance 엔티티 목록
     */
    List<Attendance> findByUserIdAndAttendanceDateBetween(Long userId, LocalDate startDate, LocalDate endDate);
}

