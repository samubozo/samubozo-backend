package com.playdata.attendanceservice.absence.repository;

import com.playdata.attendanceservice.absence.entity.Absence;
import com.playdata.attendanceservice.absence.entity.ApprovalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {

    /**
     * 특정 사용자의 모든 부재 내역을 조회합니다.
     */
    List<Absence> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 결재 상태별 부재 내역을 조회합니다.
     */
    List<Absence> findByApprovalStatusOrderByCreatedAtDesc(ApprovalStatus approvalStatus);

    /**
     * 특정 사용자의 결재 상태별 부재 내역을 조회합니다.
     */
    List<Absence> findByUserIdAndApprovalStatusOrderByCreatedAtDesc(Long userId, ApprovalStatus approvalStatus);

    /**
     * 결재자별 처리한 부재 내역을 조회합니다.
     */
    List<Absence> findByApproverIdOrderByApprovedAtDesc(Long approverId);

    /**
     * 날짜 범위별 부재 내역을 조회합니다.
     */
    @Query("SELECT a FROM Absence a WHERE a.startDate BETWEEN :startDate AND :endDate ORDER BY a.createdAt DESC")
    List<Absence> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 대기 중인 부재 내역을 페이지네이션으로 조회합니다. (결재용)
     */
    @Query("SELECT a FROM Absence a WHERE a.approvalStatus = 'PENDING' ORDER BY a.createdAt DESC")
    Page<Absence> findPendingAbsences(Pageable pageable);

    /**
     * 처리된 부재 내역을 페이지네이션으로 조회합니다. (결재용)
     */
    @Query("SELECT a FROM Absence a WHERE a.approvalStatus IN ('APPROVED', 'REJECTED') ORDER BY a.updatedAt DESC")
    Page<Absence> findProcessedAbsences(Pageable pageable);

    /**
     * 결재 상태별 부재 개수를 조회합니다.
     */
    long countByApprovalStatus(ApprovalStatus approvalStatus);

    /**
     * 특정 사용자의 승인된 부재 내역을 조회합니다.
     */
    List<Absence> findByUserIdAndApprovalStatus(Long userId, ApprovalStatus approvalStatus);

    /**
     * 특정 기간 동안의 부재 내역을 조회합니다.
     */
    @Query("SELECT a FROM Absence a WHERE a.userId = :userId AND a.startDate <= :endDate AND a.endDate >= :startDate ORDER BY a.startDate")
    List<Absence> findByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 특정 사용자의 특정 날짜 부재 내역을 조회합니다.
     */
    @Query("SELECT a FROM Absence a WHERE a.userId = :userId AND a.startDate <= :date AND a.endDate >= :date AND a.approvalStatus = 'APPROVED'")
    List<Absence> findByUserIdAndDate(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 특정 사용자의 승인된 부재 개수를 조회합니다.
     */
    long countByUserIdAndApprovalStatus(Long userId, ApprovalStatus approvalStatus);
}
