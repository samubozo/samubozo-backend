package com.playdata.approvalservice.approval.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@Builder
@Entity
@Table(name = "approval_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false)
    private RequestType requestType;

    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Setter
    @Column(name = "approver_id")
    private Long approverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApprovalStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    @Setter
    private LocalDateTime processedAt;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "title")
    private String title;

    @Column(name = "vacations_id")
    private Long vacationsId;

    @Column(name = "vacation_type")
    private String vacationType;

    @Column(name = "certificates_id")
    private Long certificateId;

    @Enumerated(EnumType.STRING)
    @Column(name = "certificate_type") // [추가] 증명서 종류를 저장하기 위한 필드
    private Type certificateType;

    @Column(name = "start_date")
    private java.time.LocalDate startDate;

    @Column(name = "end_date")
    private java.time.LocalDate endDate;

    @Setter
    @Column(name = "reject_comment")
    private String rejectComment;

    // ===== 부재 관련 필드들 추가 =====
    @Column(name = "absences_id")
    private Long absencesId; // absence-service의 부재 ID

    @Enumerated(EnumType.STRING)
    @Column(name = "absence_type")
    private AbsenceType absenceType; // 부재 종류

    @Enumerated(EnumType.STRING)
    @Column(name = "urgency")
    private UrgencyType urgency; // 긴급도

    @Column(name = "start_time")
    private java.time.LocalTime startTime; // 부재 시작 시간

    @Column(name = "end_time")
    private java.time.LocalTime endTime; // 부재 종료 시간

    // 결재 상태 변경 메소드
    public void approve() {
        this.status = ApprovalStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(String rejectComment) {
        this.status = ApprovalStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
        this.rejectComment = rejectComment;
    }

}