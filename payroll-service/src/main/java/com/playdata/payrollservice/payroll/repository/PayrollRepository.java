package com.playdata.payrollservice.payroll.repository;

import com.playdata.payrollservice.payroll.entity.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    // 사용자 ID로 급여 정보 조회
    Optional<Payroll> findByUserId(Long userId);

    // 모든 사용자 급여 정보 조회
    List<Payroll> findAllByOrderByUpdatedAtDesc();
}
