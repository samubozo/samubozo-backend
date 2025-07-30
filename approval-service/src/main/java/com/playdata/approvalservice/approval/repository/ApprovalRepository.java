package com.playdata.approvalservice.approval.repository;

import com.playdata.approvalservice.approval.entity.ApprovalRequest;
import com.playdata.approvalservice.approval.entity.ApprovalStatus;
import com.playdata.approvalservice.approval.entity.RequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalRepository extends JpaRepository<ApprovalRequest, Long> {
    // 기존 메서드들
    // 특정 사용자의 특정 날짜에 승인된 휴가 요청을 조회
    Optional<ApprovalRequest> findByApplicantIdAndRequestedAtBetweenAndStatus(
            Long applicantId, LocalDateTime startOfDay, LocalDateTime endOfDay, ApprovalStatus status);

    // 특정 사용자의 특정 날짜에 승인된 모든 요청을 조회 (휴가 외 다른 요청도 있을 수 있으므로 List 반환)
    List<ApprovalRequest> findAllByApplicantIdAndRequestedAtBetweenAndStatus(
            Long applicantId, LocalDateTime startOfDay, LocalDateTime endOfDay, ApprovalStatus status);

    // 특정 상태의 모든 결재 요청을 조회
    List<ApprovalRequest> findByStatus(ApprovalStatus status);

    // 특정 결재자가 처리한 (승인 또는 반려) 모든 결재 요청을 조회
    List<ApprovalRequest> findByApproverIdAndStatusInOrderByProcessedAtDesc(Long approverId, List<ApprovalStatus> statuses);

    // 특정 사용자의 특정 날짜 범위 내에 특정 상태를 가진 요청이 있는지 조회
    List<ApprovalRequest> findByApplicantIdAndRequestedAtBetweenAndStatusIn(
            Long applicantId, LocalDateTime startOfDay, LocalDateTime endOfDay, List<ApprovalStatus> statuses);

    // 특정 사용자의 특정 요청 유형에 대해 특정 날짜 범위 내에 특정 상태를 가진 요청이 있는지 조회
    List<ApprovalRequest> findByApplicantIdAndRequestTypeAndRequestedAtBetweenAndStatusIn(
            Long applicantId, RequestType requestType, LocalDateTime startOfDay, LocalDateTime endOfDay, List<ApprovalStatus> statuses);

    // 특정 요청 유형의 모든 결재 요청을 조회
    List<ApprovalRequest> findByRequestType(RequestType requestType);

    // ===== 부재 관련 메서드들 =====

    /**
     * 부재 결재 요청 목록 조회 (최신순)
     */
    List<ApprovalRequest> findByRequestTypeOrderByRequestedAtDesc(RequestType requestType);

    /**
     * 대기 중인 부재 결재 요청 목록 조회 (페이징)
     */
    Page<ApprovalRequest> findByRequestTypeAndStatusOrderByRequestedAtDesc(
            RequestType requestType, ApprovalStatus status, Pageable pageable);

    /**
     * 처리된 부재 결재 요청 목록 조회 (페이징)
     */
    @Query("SELECT ar FROM ApprovalRequest ar WHERE ar.requestType = 'ABSENCE' AND ar.status IN ('APPROVED', 'REJECTED') ORDER BY ar.processedAt DESC")
    Page<ApprovalRequest> findProcessedAbsenceApprovalRequests(Pageable pageable);

    /**
     * 특정 사용자의 부재 결재 요청 목록 조회
     */
    List<ApprovalRequest> findByRequestTypeAndApplicantIdOrderByRequestedAtDesc(
            RequestType requestType, Long applicantId);

    /**
     * 특정 결재자가 처리한 부재 결재 요청 목록 조회
     */
    List<ApprovalRequest> findByRequestTypeAndApproverIdOrderByProcessedAtDesc(
            RequestType requestType, Long approverId);

    /**
     * 부재 결재 요청 통계
     */
    @Query("SELECT COUNT(ar) FROM ApprovalRequest ar WHERE ar.requestType = 'ABSENCE'")
    long countAbsenceApprovalRequests();

    @Query("SELECT COUNT(ar) FROM ApprovalRequest ar WHERE ar.requestType = 'ABSENCE' AND ar.status = 'PENDING'")
    long countPendingAbsenceApprovalRequests();

    @Query("SELECT COUNT(ar) FROM ApprovalRequest ar WHERE ar.requestType = 'ABSENCE' AND ar.status = 'APPROVED'")
    long countApprovedAbsenceApprovalRequests();

    @Query("SELECT COUNT(ar) FROM ApprovalRequest ar WHERE ar.requestType = 'ABSENCE' AND ar.status = 'REJECTED'")
    long countRejectedAbsenceApprovalRequests();
}