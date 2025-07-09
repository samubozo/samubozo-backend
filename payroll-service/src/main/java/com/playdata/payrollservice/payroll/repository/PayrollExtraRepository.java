package com.playdata.payrollservice.payroll.repository;

import com.playdata.payrollservice.payroll.entity.PayrollExtra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollExtraRepository extends JpaRepository<PayrollExtra, Long> {

    List<PayrollExtra> findByUserId(Long userId);
}
