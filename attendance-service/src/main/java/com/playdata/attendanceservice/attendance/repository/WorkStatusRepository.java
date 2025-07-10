package com.playdata.attendanceservice.attendance.repository;

import com.playdata.attendanceservice.attendance.entity.WorkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkStatusRepository extends JpaRepository<WorkStatus, Long> {
    Optional<WorkStatus> findByAttendanceId(Long attendanceId);
}
