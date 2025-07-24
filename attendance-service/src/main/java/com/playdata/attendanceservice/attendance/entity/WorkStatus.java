package com.playdata.attendanceservice.attendance.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "work_statuses", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "date"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkStatus extends com.playdata.attendanceservice.common.domain.BaseEntity {

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
    private LocalTime checkInTime;


    @Column(name = "check_out_time")
    private LocalTime checkOutTime;

    @Column(name = "out_time")
    private LocalTime outTime;

    @Column(name = "return_time")
    private LocalTime returnTime;

    @JsonIgnore // JSON 직렬화 시 이 필드를 무시하여 순환 참조를 방지합니다.
    @OneToOne
    @JoinColumn(name = "attendance_id")
    private Attendance attendance;

    @Column(name = "is_late", nullable = false)
    private boolean isLate; // 지각 여부

    @Builder
    public WorkStatus(Long id, Long userId, LocalDate date, WorkStatusType statusType, WorkDayType workDayType, String reason, LocalTime checkInTime, LocalTime checkOutTime, LocalTime outTime, LocalTime returnTime, Attendance attendance, boolean isLate) {
        this.id = id;
        this.userId = userId;
        this.date = date;
        this.statusType = statusType;
        this.workDayType = workDayType;
        this.reason = reason;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.outTime = outTime;
        this.returnTime = returnTime;
        this.attendance = attendance;
        this.isLate = isLate;
    }

    // 새로운 생성자: Attendance 객체를 직접 받아 필요한 정보를 설정합니다.
    public WorkStatus(Attendance attendance, WorkStatusType type, String reason) {
        this.attendance = attendance;
        this.userId = attendance.getUserId();
        this.date = attendance.getAttendanceDate();
        this.statusType = type;
        this.reason = reason;
    }


    public WorkStatus(Long userId, LocalDate date, WorkStatusType type, String reason) {
        this.userId = userId;
        this.date = date;
        this.statusType = type;
        this.reason = reason;
    }

    // 지각 기록
    public static WorkStatus recordLate(Long userId, LocalDate date, LocalTime checkIn, String reason) {
        WorkStatus status = new WorkStatus(userId, date, WorkStatusType.LATE, reason);
        status.checkInTime = checkIn;
        return status;
    }

    // 조퇴 기록
    public static WorkStatus recordEarlyLeave(Long userId, LocalDate date, LocalTime checkOut, String reason) {
        WorkStatus status = new WorkStatus(userId, date, WorkStatusType.EARLY_LEAVE, reason);
        status.checkOutTime = checkOut;
        return status;
    }

    public void setAttendance(Attendance attendance) {
        this.attendance = attendance;
    }

    public void setCheckInTime(LocalTime checkInTime) {
        this.checkInTime = checkInTime;
    }

    public void setCheckOutTime(LocalTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    public void setStatusType(WorkStatusType statusType) {
        this.statusType = statusType;
    }

    public void setWorkDayType(WorkDayType workDayType) {
        this.workDayType = workDayType;
    }

    
}
