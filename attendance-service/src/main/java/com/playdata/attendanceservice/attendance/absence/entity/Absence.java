package com.playdata.attendanceservice.attendance.absence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "absence")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Absence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AbsenceType type;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    // 시간 단위 부재(반차, 외출 등)를 위한 필드
    private LocalTime startTime;
    private LocalTime endTime;

    private String reason;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public Absence(String userId, AbsenceType type, LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime, String reason) {
        this.userId = userId;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
    }

    public void update(AbsenceType type, LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime, String reason) {
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
    }
}
