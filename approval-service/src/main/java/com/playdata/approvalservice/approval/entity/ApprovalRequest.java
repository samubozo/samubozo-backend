package com.playdata.approvalservice.approval.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @Column(name = "approver_id")
    private Long approverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApprovalStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "vacations_id")
    private Long vacationsId;

    @Column(name = "certificates_id")
    private Long certificatesId;

    @Builder
    public ApprovalRequest(RequestType requestType, Long applicantId, Long approverId, ApprovalStatus status, LocalDateTime requestedAt, LocalDateTime approvedAt, String reason, Long vacationsId, Long certificatesId) {
        this.requestType = requestType;
        this.applicantId = applicantId;
        this.approverId = approverId;
        this.status = status;
        this.requestedAt = requestedAt;
        this.approvedAt = approvedAt;
        this.reason = reason;
        this.vacationsId = vacationsId;
        this.certificatesId = certificatesId;
    }

    // 결재 상태 변경 메소드
    public void approve() {
        this.status = ApprovalStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = ApprovalStatus.REJECTED;
        this.approvedAt = LocalDateTime.now(); // 반려 시점도 기록
    }
}
