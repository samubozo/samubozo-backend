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

    /**
     * 특정 연도와 월에 모든 사용자의 근무일수를 계산합니다.
     * 정상 출근(ON_TIME)과 지각(LATE)만 근무일로 인정합니다.
     *
     * @param year 조회할 연도
     * @param month 조회할 월
     * @param workStatuses 근무일로 인정할 근무 상태 목록
     * @return 각 사용자별 근무일수 정보를 담은 DTO 목록
     */
    @Query("SELECT new com.playdata.attendanceservice.attendance.dto.MonthlyWorkDaysResponse(a.userId, COUNT(a)) " +
           "FROM Attendance a " +
           "WHERE YEAR(a.attendanceDate) = :year AND MONTH(a.attendanceDate) = :month " +
           "AND a.workStatus.statusType IN :workStatuses " +
           "GROUP BY a.userId")
    List<com.playdata.attendanceservice.attendance.dto.MonthlyWorkDaysResponse> countWorkDaysByMonth(@Param("year") int year, @Param("month") int month, @Param("workStatuses") List<WorkStatusType> workStatuses);
}

