package com.playdata.attendanceservice.attendance.absence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // 추가
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "absence")
@Getter
@Setter(AccessLevel.PROTECTED) // 추가: 같은 패키지에서만 setter 사용 가능
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Absence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId; // String → Long으로 변경

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AbsenceType type;

    @Enumerated(EnumType.STRING)
    private UrgencyType urgency;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    // 시간 단위 부재(반차, 외출 등)를 위한 필드
    private LocalTime startTime;
    private LocalTime endTime;

    private String reason;

    @Setter // AbsenceService에서 접근 가능하도록 public setter 추가
    @Column
    private Long approvalRequestId; // 결재 요청 ID 추가

    // 결재 관련 필드 추가
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column
    private Long approverId; // String → Long으로 변경

    @Column
    private LocalDateTime approvedAt;

    @Column
    private String rejectComment;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public Absence(Long userId, AbsenceType type, UrgencyType urgency, LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime, String reason) {
        this.userId = userId;
        this.type = type;
        this.urgency = urgency;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
        this.approvalStatus = ApprovalStatus.PENDING;
    }

    public void update(AbsenceType type, UrgencyType urgency, LocalDate startDate, LocalDate endDate, LocalTime startTime, LocalTime endTime, String reason) {
        this.type = type;
        this.urgency = urgency;
        this.startDate = startDate;
        this.endDate = endDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.reason = reason;
    }

    // 결재 승인 메서드
    public void approve(Long approverId) { // String → Long으로 변경
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.approverId = approverId;
        this.approvedAt = LocalDateTime.now();
    }

    // 결재 반려 메서드
    public void reject(Long approverId, String rejectComment) { // String → Long으로 변경
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.approverId = approverId;
        this.rejectComment = rejectComment;
        this.approvedAt = LocalDateTime.now();
    }

    // 대기 상태인지 확인
    public boolean isPending() {
        return this.approvalStatus == ApprovalStatus.PENDING;
    }

    // 승인 상태인지 확인
    public boolean isApproved() {
        return this.approvalStatus == ApprovalStatus.APPROVED;
    }

    // 반려 상태인지 확인
    public boolean isRejected() {
        return this.approvalStatus == ApprovalStatus.REJECTED;
    }

    // 편의 메서드 추가
    public boolean requiresApproval() {
        return this.type.requiresApproval();
    }

    public boolean isSelfApproved() {
        return this.type.isSelfApproved();
    }
}