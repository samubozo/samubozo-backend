package com.playdata.attendanceservice.attendance.repository;

import com.playdata.attendanceservice.attendance.entity.Vacation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VacationRepository extends JpaRepository<Vacation, Long> {
}
