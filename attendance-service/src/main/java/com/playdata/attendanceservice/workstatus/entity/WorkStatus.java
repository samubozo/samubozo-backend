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

    public void setIsLate(boolean isLate) {
        this.isLate = isLate;
    }

    // 필요한 setter 메서드 추가
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

    // 지각 기록
    public static WorkStatus recordLate(Long userId, LocalDate date, LocalDateTime checkIn, String reason) {
        return WorkStatus.builder()
                .userId(userId)
                .date(date)
                .statusType(WorkStatusType.LATE)
                .checkInTime(checkIn)
                .reason(reason)
                .isLate(true) // 지각이므로 isLate는 true
                .build();
    }

    // 조퇴 기록
    public static WorkStatus recordEarlyLeave(Long userId, LocalDate date, LocalDateTime checkOut, String reason) {
        return WorkStatus.builder()
                .userId(userId)
                .date(date)
                .statusType(WorkStatusType.EARLY_LEAVE)
                .checkOutTime(checkOut)
                .reason(reason)
                .isLate(false) // 조퇴는 지각이 아님
                .build();
    }

    // 편의 메서드들 (선택사항)
    public void setAbsenceStatus(WorkStatusType statusType, String reason) {
        this.statusType = statusType;
        this.reason = reason;
        this.isLate = false; // 부재는 지각 아님
    }

    public void setVacationStatus(WorkStatusType statusType, String reason) {
        this.statusType = statusType;
        this.reason = reason;
        this.isLate = false; // 휴가는 지각 아님
    }
}
