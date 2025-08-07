package com.playdata.attendanceservice.workstatus.repository;

import com.playdata.attendanceservice.workstatus.entity.WorkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WorkStatusRepository extends JpaRepository<WorkStatus, Long> {
    Optional<WorkStatus> findByAttendanceId(Long attendanceId);
    List<WorkStatus> findByUserIdInAndDateBetween(List<Long> userIds, LocalDate startDate, LocalDate endDate);
    Optional<WorkStatus> findByUserIdAndDate(Long userId, LocalDate date);
    List<WorkStatus> findByUserId(Long userId);
    List<WorkStatus> findByDateBetween(LocalDate startDate, LocalDate endDate);
}
