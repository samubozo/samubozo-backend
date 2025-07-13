package com.playdata.vacationservice.vacation.repository;

import com.playdata.vacationservice.vacation.entity.Vacation; // 경로 변경
import org.springframework.data.jpa.repository.JpaRepository;

public interface VacationRepository extends JpaRepository<Vacation, Long> {
}