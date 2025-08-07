package com.playdata.attendanceservice.absence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "absence")
@Getter
@Setter(AccessLevel.PROTECTED)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Absence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AbsenceType type;

    @Enumerated(EnumType.STRING)
    private UrgencyType urgency;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    private LocalTime startTime;
    private LocalTime endTime;

    private String reason;

    @Setter
    @Column
    private Long approvalRequestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column
    private Long approverId;

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

    public void approve(Long approverId) {
        this.approvalStatus = ApprovalStatus.APPROVED;
        this.approverId = approverId;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject(Long approverId, String rejectComment) {
        this.approvalStatus = ApprovalStatus.REJECTED;
        this.approverId = approverId;
        this.rejectComment = rejectComment;
        this.approvedAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return this.approvalStatus == ApprovalStatus.PENDING;
    }

    public boolean isApproved() {
        return this.approvalStatus == ApprovalStatus.APPROVED;
    }

    public boolean isRejected() {
        return this.approvalStatus == ApprovalStatus.REJECTED;
    }

    public boolean requiresApproval() {
        return this.type.requiresApproval();
    }

    public boolean isSelfApproved() {
        return this.type.isSelfApproved();
    }
}
