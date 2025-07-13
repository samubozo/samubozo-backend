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
}