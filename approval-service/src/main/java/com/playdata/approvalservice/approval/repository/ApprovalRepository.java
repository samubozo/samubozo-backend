package com.playdata.approvalservice.approval.repository;

import com.playdata.approvalservice.approval.entity.ApprovalRequest;
import com.playdata.approvalservice.approval.entity.ApprovalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalRepository extends JpaRepository<ApprovalRequest, Long> {
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
}