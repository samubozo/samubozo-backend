package com.playdata.payrollservice.payroll.repository;

import com.playdata.payrollservice.payroll.entity.Salary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryRepository extends JpaRepository<Salary, Long> {

    // 사용자 ID로 급여 정보 조회
    Optional<Salary> findByUserId(Long userId);

    // 모든 사용자 급여 정보 조회
    List<Salary> findAllByOrderByUpdatedAtDesc();
}
