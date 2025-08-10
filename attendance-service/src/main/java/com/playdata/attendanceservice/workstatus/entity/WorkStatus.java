package com.playdata.attendanceservice.workstatus.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.playdata.attendanceservice.attendance.entity.Attendance;
import com.playdata.attendanceservice.attendance.entity.WorkDayType;
import com.playdata.attendanceservice.common.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "work_statuses", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "date"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class WorkStatus extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_type", nullable = false)
    private WorkStatusType statusType;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_day_type")
    private WorkDayType workDayType;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_out_time")
    private LocalDateTime checkOutTime;

    

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "attendance_id")
    private Attendance attendance;

    @Column(name = "is_late", nullable = false)
    private boolean isLate;

    

    
    public void setAttendance(Attendance attendance) {
        this.attendance = attendance;
    }

    public void setCheckInTime(LocalDateTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public void setCheckOutTime(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public void setStatusType(WorkStatusType statusType) {
        this.statusType = statusType;
    }

    public void setWorkDayType(WorkDayType workDayType) {
        this.workDayType = workDayType;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    

    

    
}
