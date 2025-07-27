package com.playdata.approvalservice.approval.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // 추가

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

    @Setter // 추가
    @Column(name = "approver_id")
    private Long approverId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApprovalStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "processed_at")
    @Setter // 추가
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

    @Column(name = "start_date") // 추가
    private java.time.LocalDate startDate; // 추가

    @Column(name = "end_date") // 추가
    private java.time.LocalDate endDate; // 추가

    @Setter
    @Column(name = "reject_comment")
    private String rejectComment;

    @Builder
    public ApprovalRequest(RequestType requestType, Long applicantId, Long approverId, ApprovalStatus status, LocalDateTime requestedAt, LocalDateTime processedAt, String reason, String title, Long vacationsId, String vacationType, Long certificatesId, java.time.LocalDate startDate, java.time.LocalDate endDate, String rejectComment) {
        this.requestType = requestType;
        this.applicantId = applicantId;
        this.approverId = approverId;
        this.status = status;
        this.requestedAt = requestedAt;
        this.processedAt = processedAt;
        this.reason = reason;
        this.title = title;
        this.vacationsId = vacationsId;
        this.vacationType = vacationType;
        this.certificateId = certificatesId;
        this.startDate = startDate; // 빌더에 추가
        this.endDate = endDate; // 빌더에 추가
        this.rejectComment = rejectComment;
    }

    // 결재 상태 변경 메소드
    public void approve() {
        this.status = ApprovalStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
    }

    public void reject(String rejectComment) {
        this.status = ApprovalStatus.REJECTED;
        this.processedAt = LocalDateTime.now(); // 반려 시점도 기록
        this.rejectComment = rejectComment;
    }
}
