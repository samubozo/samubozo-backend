package com.playdata.vacationservice.vacation.entity;

import com.playdata.vacationservice.common.domain.BaseEntity; // 경로 변경
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
// import lombok.Setter; // 제거

import java.time.LocalDate;

@Entity
@Table(name = "vacations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vacation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // @Setter(AccessLevel.PACKAGE) // 제거
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "vacation_type", nullable = false)
    private VacationType vacationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "vacation_status", nullable = false)
    @Setter
    private VacationStatus vacationStatus;

    @Column(name = "reason")
    private String reason;

    @Builder
    public Vacation(Long userId, LocalDate startDate, LocalDate endDate, VacationType vacationType, VacationStatus vacationStatus, String reason) {
        this.userId = userId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.vacationType = vacationType;
        this.vacationStatus = vacationStatus;
        this.reason = reason;
    }
}