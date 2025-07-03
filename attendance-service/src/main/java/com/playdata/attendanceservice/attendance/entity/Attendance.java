package com.playdata.attendanceservice.attendance.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "attendances")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attendance {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate date;

    @Column(name = "check_in_time")
    private LocalTime checkInTime;

    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    @Column(name = "ip_address")
    private String ipAddress;

    public Attendance(Long userId, LocalDate date, String ipAddress) {
        this.userId = userId;
        this.date = date;
        this.ipAddress = ipAddress;
    }

    public void checkIn(LocalTime time) {
        this.checkInTime = time;
    }

    public void checkOut(LocalTime time) {
        this.checkOutTime = time;
    }




}
