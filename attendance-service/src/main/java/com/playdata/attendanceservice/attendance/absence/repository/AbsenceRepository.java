package com.playdata.attendanceservice.attendance.absence.repository;

import com.playdata.attendanceservice.attendance.absence.entity.Absence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    /**
     * 특정 사용자의 모든 부재 내역을 조회합니다.
     * @param userId 사용자 ID
     * @return 해당 사용자의 부재 내역 리스트
     */
    List<Absence> findByUserId(String userId);
}
