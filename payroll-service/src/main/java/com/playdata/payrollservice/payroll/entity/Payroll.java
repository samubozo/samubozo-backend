package com.playdata.payrollservice.payroll.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payrolls")
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_id")
    private Long payrollId;


    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "base_payroll")
    private Integer basePayroll;

    @Column(name = "position_allowance")
    private Integer positionAllowance;

    @Column(name = "meal_allowance")
    private Integer mealAllowance;

    @Column(name = "bonus")
    private Integer bonus;

    @Column(name = "pay_year", nullable = false)
    private Integer payYear;

    @Column(name = "pay_month", nullable = false)
    private Integer payMonth;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;





}
