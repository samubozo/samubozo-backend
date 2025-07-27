package com.playdata.certificateservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime; // LocalDateTime import 추가

import static com.playdata.certificateservice.entity.Status.PENDING;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "tbl_certificates")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "certificate_id", nullable = false)
    private Long certificateId;

    @Column(name = "employee_no", nullable = false)
    private Long employeeNo;

    @Enumerated(EnumType.STRING)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = PENDING;

    @Column(columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "request_date")
    private LocalDate requestDate;

    @Column(name = "approve_date")
    private LocalDate approveDate;

    @Column(name = "approval_request_id")
    private Long approvalRequestId;

    @Column(name = "reject_comment", columnDefinition = "TEXT")
    private String rejectComment;

    @Column(name = "approver_id")
    private Long approverId; // 추가: 결재자 ID

    @Column(name = "approver_name")
    private String approverName; // 추가: 결재자 이름

    @Column(name = "processed_at")
    private LocalDateTime processedAt; // 추가: 처리 일시

    // 결재 처리 시 필드 업데이트 메서드
    public void approve(Long approverId, String approverName) {
        this.status = Status.APPROVED;
        this.approverId = approverId;
        this.approverName = approverName;
        this.processedAt = LocalDateTime.now();
        this.approveDate = LocalDate.now(); // 승인일자도 업데이트
    }

    // 반려 처리 시 필드 업데이트 메서드
    public void reject(Long approverId, String rejectComment, String approverName) {
        this.status = Status.REJECTED;
        this.approverId = approverId;
        this.rejectComment = rejectComment;
        this.approverName = approverName;
        this.processedAt = LocalDateTime.now();
    }
}
