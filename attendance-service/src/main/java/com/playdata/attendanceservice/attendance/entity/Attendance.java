package com.playdata.attendanceservice.attendance.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 근태 기록을 나타내는 엔티티 클래스입니다.
 * 데이터베이스의 'attendance' 테이블과 매핑됩니다.
 * 한 사용자는 하루에 하나의 근태 기록만 가질 수 있도록 복합 유니크 제약 조건을 설정합니다.
 */
@Entity // 이 클래스가 JPA 엔티티임을 나타냅니다.
@Table(name = "attendance", // 매핑될 데이터베이스 테이블 이름을 'attendance'로 지정합니다.
       uniqueConstraints = { // 복합 유니크 제약 조건을 정의합니다.
           @UniqueConstraint(name = "UK_user_id_attendance_date", columnNames = {"user_id", "attendance_date"})
       })
@Getter // Lombok: 모든 필드에 대한 getter 메소드를 자동으로 생성합니다.
@Builder // Lombok: 빌더 패턴을 사용하여 객체를 생성할 수 있도록 합니다.
@NoArgsConstructor(access = AccessLevel.PROTECTED) // Lombok: 인자 없는 생성자를 자동으로 생성합니다. JPA에서 엔티티는 기본 생성자가 필요합니다.
@AllArgsConstructor // Lombok: 모든 필드를 인자로 받는 생성자를 자동으로 생성합니다.
public class Attendance {

    /**
     * 근태 기록의 고유 식별자 (Primary Key)
     * 데이터베이스에서 자동으로 생성됩니다.
     */
    @Id // 이 필드가 엔티티의 기본 키임을 나타냅니다.
    @GeneratedValue(strategy = GenerationType.IDENTITY) // 기본 키 값을 데이터베이스가 자동으로 생성하도록 합니다. (AUTO_INCREMENT)
    @Column(name = "id", nullable = false) // 'id' 컬럼에 매핑되며, NULL을 허용하지 않습니다.
    private Long id;

    /**
     * 근태 기록의 대상이 되는 사용자 ID
     */
    @Column(name = "user_id", nullable = false) // 'user_id' 컬럼에 매핑되며, NULL을 허용하지 않습니다.
    private Long userId;

    /**
     * 근태 기록이 발생한 날짜
     * user_id와 함께 복합 유니크 제약 조건의 일부입니다.
     */
    @Column(name = "attendance_date", nullable = false) // 'attendance_date' 컬럼에 매핑되며, NULL을 허용하지 않습니다.
    private LocalDate attendanceDate;

    /**
     * 출근 시간
     * 엔티티가 생성될 때 자동으로 현재 시간이 기록됩니다.
     */
    @CreationTimestamp // 엔티티가 생성될 때 현재 시간을 자동으로 설정합니다.
    @Column(name = "check_in_time", nullable = false) // 'check_in_time' 컬럼에 매핑되며, NULL을 허용하지 않습니다.
    private LocalDateTime checkInTime;

    /**
     * 퇴근 시간
     * 엔티티가 업데이트될 때 자동으로 현재 시간이 기록될 수 있습니다. (초기에는 NULL)
     */
    @UpdateTimestamp // 엔티티가 업데이트될 때 현재 시간을 자동으로 설정합니다.
    @Column(name = "check_out_time") // 'check_out_time' 컬럼에 매핑됩니다. NULL을 허용합니다.
    private LocalDateTime checkOutTime;

    /**
     * 근태 기록 시 사용된 IP 주소
     */
    @Column(name = "ip_address", nullable = false) // 'ip_address' 컬럼에 매핑되며, NULL을 허용하지 않습니다.
    private String ipAddress;

    /**
     * 이 근태 기록과 연관된 상세 근무 상태 (정상, 지각, 조퇴, 부재 등)
     * Attendance와 WorkStatus는 1:1 관계를 가집니다.
     * mappedBy는 WorkStatus 엔티티의 attendance 필드에 의해 매핑됨을 나타냅니다.
     * CascadeType.ALL은 Attendance 엔티티의 변경이 WorkStatus 엔티티에도 전파되도록 합니다.
     * OrphanRemoval = true는 Attendance 엔티티가 삭제될 때 연관된 WorkStatus 엔티티도 함께 삭제되도록 합니다.
     */
    @OneToOne(mappedBy = "attendance", cascade = CascadeType.ALL, orphanRemoval = true)
    private WorkStatus workStatus;

    /**
     * 퇴근 시간을 업데이트하는 메소드입니다.
     *
     * @param checkOutTime 업데이트할 퇴근 시간
     */
    public void updateCheckOutTime(LocalDateTime checkOutTime) {
        this.checkOutTime = checkOutTime;
    }

    /**
     * WorkStatus 엔티티를 설정하는 메소드입니다.
     * 양방향 관계 설정을 위해 WorkStatus 엔티티에도 이 Attendance 엔티티를 설정합니다.
     * @param workStatus 설정할 WorkStatus 엔티티
     */
    public void setWorkStatus(WorkStatus workStatus) {
        this.workStatus = workStatus;
        if (workStatus != null) {
            workStatus.setAttendance(this);
        }
    }
}
