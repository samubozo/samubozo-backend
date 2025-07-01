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
@Table(name = "salaries")
public class Salary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "salary_id")
    private Long salaryId;


    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "base_salary")
    private Integer baseSalary;

    @Column(name = "position_allowance")
    private Integer positionAllowance;

    @Column(name = "meal_allowance")
    private Integer mealAllowance;

    @Column(name = "updated_at")
    @UpdateTimestamp
    private LocalDateTime updatedAt;





}
