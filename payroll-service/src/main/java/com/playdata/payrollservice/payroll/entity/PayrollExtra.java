package com.playdata.payrollservice.payroll.entity;

import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "payroll_extras")
public class PayrollExtra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "extra_id")
    private Long extraId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    private Integer amount;
    private String description;

    @Column(name = "date_given")
    private LocalDate dateGiven;
}
