package com.playdata.approvalservice.approval.repository;

import com.playdata.approvalservice.approval.entity.ApprovalRequest;
import com.playdata.approvalservice.approval.entity.ApprovalStatus;
import com.playdata.approvalservice.approval.entity.RequestType;
import com.playdata.approvalservice.approval.entity.Type;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface ApprovalRepository extends JpaRepository<ApprovalRequest, Long>, JpaSpecificationExecutor<ApprovalRequest> {

    // ===== 기본 조회 메서드들 =====

    

    /**
     * 특정 사용자의 특정 날짜에 승인된 모든 요청을 조회
     */
    List<ApprovalRequest> findAllByApplicantIdAndRequestedAtBetweenAndStatus(
            Long applicantId, LocalDateTime startOfDay, LocalDateTime endOfDay, ApprovalStatus status);

    /**
     * 특정 상태의 모든 결재 요청을 조회 (최신순)
     */
    List<ApprovalRequest> findByStatusOrderByRequestedAtDesc(ApprovalStatus status);

    /**
     * 특정 결재자가 처리한 모든 결재 요청을 조회 (최신순)
     */
    List<ApprovalRequest> findByApproverIdAndStatusInOrderByProcessedAtDesc(
            Long approverId, List<ApprovalStatus> statuses);

    /**
     * 중복 검증을 위한 메서드 (기존 요청 생성 시간 기준)
     */
    List<ApprovalRequest> findByApplicantIdAndRequestTypeAndRequestedAtBetweenAndStatusIn(
            Long applicantId, RequestType requestType, LocalDateTime startOfDay, LocalDateTime endOfDay,
            List<ApprovalStatus> statuses);

    /**
     * 특정 신청자의 특정 요청 유형에 대해, 주어진 기간과 겹치는 대기 중이거나 승인된 결재 요청을 조회합니다.
     * @param applicantId 신청자 ID
     * @param requestType 요청 유형 (예: ABSENCE)
     * @param newStartDate 새로운 요청의 시작일
     * @param newEndDate 새로운 요청의 종료일
     * @param statuses 조회할 결재 상태 목록 (PENDING, APPROVED)
     * @return 겹치는 결재 요청 목록
     */
    @Query("SELECT ar FROM ApprovalRequest ar WHERE ar.applicantId = :applicantId " +
            "AND ar.requestType = :requestType " +
            "AND ar.status IN :statuses " +
            "AND ((ar.startDate <= :newEndDate AND ar.endDate >= :newStartDate)) ")
    List<ApprovalRequest> findOverlappingRequests(
            @Param("applicantId") Long applicantId,
            @Param("requestType") RequestType requestType,
            @Param("newStartDate") LocalDate newStartDate,
            @Param("newEndDate") LocalDate newEndDate,
            @Param("statuses") List<ApprovalStatus> statuses);

    /**
     * [신규 추가] 특정 사용자의 특정 기간에 겹치는 모든 '대기' 또는 '승인' 상태의 결재 요청을 조회합니다.
     * (휴가, 부재 등 요청 유형과 관계없이 모든 요청을 검사합니다.)
     * @param applicantId 신청자 ID
     * @param startDate   새로운 요청의 시작일
     * @param endDate     새로운 요청의 종료일
     * @param statuses    검사할 상태 목록 (PENDING, APPROVED)
     * @return 겹치는 결재 요청 목록
     */
    @Query("SELECT ar FROM ApprovalRequest ar " +
            "WHERE ar.applicantId = :applicantId " +
            "AND ar.status IN :statuses " +
            "AND ar.startDate <= :endDate " +
            "AND ar.endDate >= :startDate")
    List<ApprovalRequest> findOverlappingRequestsForUser(
            @Param("applicantId") Long applicantId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("statuses") List<ApprovalStatus> statuses
    );

    // ===== 타입별 조회 메서드들 =====

    /**
     * 특정 신청자가 동일한 유형의 증명서 요청을 이미 제출했는지 확인합니다.
     * (주로 PENDING 상태의 중복 요청을 확인하는 데 사용됩니다.)
     * @param applicantId 신청자 ID
     * @param requestType 요청 유형 (CERTIFICATE)
     * @param certificateType 증명서 유형
     * @param status 결재 상태 (PENDING)
     * @return 해당 조건에 맞는 결재 요청 (존재하지 않으면 Optional.empty())
     */
    Optional<ApprovalRequest> findByApplicantIdAndRequestTypeAndCertificateTypeAndStatus(
            Long applicantId, RequestType requestType, Type certificateType, ApprovalStatus status);

    

    /**
     * 특정 요청 유형의 모든 결재 요청을 조회 (최신순)
     */
    List<ApprovalRequest> findByRequestTypeOrderByRequestedAtDesc(RequestType requestType);

    /**
     * 특정 요청 유형과 상태의 결재 요청을 조회 (페이징)
     */
    Page<ApprovalRequest> findByRequestTypeAndStatusOrderByRequestedAtDesc(
            RequestType requestType, ApprovalStatus status, Pageable pageable);

    // ===== 부재 관련 메서드들 =====

    /**
     * 처리된 부재 결재 요청 목록 조회 (페이징)
     */
    @Query("SELECT ar FROM ApprovalRequest ar WHERE ar.requestType = :requestType " +
            "AND ar.status IN ('APPROVED', 'REJECTED') ORDER BY ar.processedAt DESC")
    Page<ApprovalRequest> findProcessedRequestsByType(@Param("requestType") RequestType requestType, Pageable pageable);

    /**
     * 특정 사용자의 특정 타입 결재 요청 목록 조회
     */
    List<ApprovalRequest> findByRequestTypeAndApplicantIdOrderByRequestedAtDesc(
            RequestType requestType, Long applicantId);

    /**
     * 특정 결재자가 처리한 특정 타입 결재 요청 목록 조회
     */
    List<ApprovalRequest> findByRequestTypeAndApproverIdOrderByProcessedAtDesc(
            RequestType requestType, Long approverId);


    /**
     * 특정 요청 유형의 모든 결재 요청을 조회합니다.
     *
     * @param requestType 조회할 요청 유형 (VACATION, CERTIFICATE, ABSENCE)
     * @return 해당 유형의 모든 결재 요청 목록
     */
    List<ApprovalRequest> findByRequestType(RequestType requestType);

    /**
     * 특정 상태의 모든 결재 요청을 조회합니다.
     *
     * @param status 조회할 결재 상태 (PENDING, APPROVED, REJECTED)
     * @return 해당 상태의 모든 결재 요청 목록
     */
    Page<ApprovalRequest> findByStatus(ApprovalStatus status, Pageable pageable);

    /**
     * 처리된 부재 결재 요청 목록을 페이징하여 조회합니다.
     * 승인(APPROVED) 또는 반려(REJECTED) 상태의 부재 결재 요청만 조회하며,
     * 처리 일시(processedAt) 기준으로 내림차순 정렬됩니다.
     *
     * @param pageable 페이징 정보 (페이지 번호, 페이지 크기)
     * @return 처리된 부재 결재 요청 페이지
     */
    @Query("SELECT ar FROM ApprovalRequest ar WHERE ar.requestType = 'ABSENCE' AND ar.status IN ('APPROVED', 'REJECTED') ORDER BY ar.processedAt DESC")
    Page<ApprovalRequest> findProcessedAbsenceApprovalRequests(Pageable pageable);

    // ===== 통계 메서드들 =====

    /**
     * 특정 타입의 결재 요청 통계
     */
    @Query("SELECT COUNT(ar) FROM ApprovalRequest ar WHERE ar.requestType = :requestType")
    long countByRequestType(@Param("requestType") RequestType requestType);

    @Query("SELECT COUNT(ar) FROM ApprovalRequest ar WHERE ar.requestType = :requestType AND ar.status = :status")
    long countByRequestTypeAndStatus(@Param("requestType") RequestType requestType, @Param("status") ApprovalStatus status);

    // ===== 부재 전용 통계 (하위 호환성 유지) =====

    @Query("SELECT COUNT(ar) FROM ApprovalRequest ar WHERE ar.requestType = 'ABSENCE'")
    long countAbsenceApprovalRequests();

    @Query("SELECT COUNT(ar) FROM ApprovalRequest ar WHERE ar.requestType = 'ABSENCE' AND ar.status = 'PENDING'")
    long countPendingAbsenceApprovalRequests();

    @Query("SELECT COUNT(ar) FROM ApprovalRequest ar WHERE ar.requestType = 'ABSENCE' AND ar.status = 'APPROVED'")
    long countApprovedAbsenceApprovalRequests();

    @Query("SELECT COUNT(ar) FROM ApprovalRequest ar WHERE ar.requestType = 'ABSENCE' AND ar.status = 'REJECTED'")
    long countRejectedAbsenceApprovalRequests();
}