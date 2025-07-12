package com.playdata.vacationservice.vacation.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "vacation_balances")
public class VacationBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "total_granted", nullable = false, precision = 4, scale = 1)
    private BigDecimal totalGranted;

    @Column(name = "used_days", nullable = false, precision = 4, scale = 1)
    private BigDecimal usedDays;

    @Builder
    public VacationBalance(Long userId, BigDecimal totalGranted, BigDecimal usedDays) {
        this.userId = userId;
        this.totalGranted = totalGranted != null ? totalGranted : BigDecimal.ZERO;
        this.usedDays = usedDays != null ? usedDays : BigDecimal.ZERO;
    }

    /**
     * 남은 연차 일수를 계산하여 반환합니다.
     *
     * @return 남은 연차 일수
     */
    public BigDecimal getRemainingDays() {
        return this.totalGranted.subtract(this.usedDays);
    }

    /**
     * 연차를 부여합니다.
     *
     * @param days 부여할 연차 일수
     */
    public void grantDays(BigDecimal days) {
        this.totalGranted = this.totalGranted.add(days);
    }

    /**
     * 연차를 사용합니다.
     *
     * @param days 사용할 연차 일수
     */
    public void useDays(BigDecimal days) {
        if (getRemainingDays().compareTo(days) < 0) {
            throw new IllegalArgumentException("남은 연차 일수가 부족합니다.");
        }
        this.usedDays = this.usedDays.add(days);
    }
}