package com.playdata.approvalservice.client;

import com.playdata.approvalservice.approval.dto.AbsenceApprovalStatisticsDto;
import com.playdata.approvalservice.approval.dto.ApprovalRequestResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AbsenceServiceClientFallback implements AbsenceServiceClient {

    @Override
    public void approveAbsence(Long absenceId, Long approverId) {
        log.error("AbsenceService approveAbsence 호출 실패 - absenceId: {}, approverId: {}", absenceId, approverId);
        throw new RuntimeException("부재 승인 처리 중 서비스 연결 오류");
    }

    @Override
    public void rejectAbsence(Long absenceId, Long approverId, String rejectComment) {
        log.error("AbsenceService rejectAbsence 호출 실패 - absenceId: {}, approverId: {}", absenceId, approverId);
        throw new RuntimeException("부재 반려 처리 중 서비스 연결 오류");
    }

    @Override
    public ApprovalRequestResponseDto getAbsenceById(Long absenceId) {
        log.error("AbsenceService getAbsenceById 호출 실패 - absenceId: {}", absenceId);
        throw new RuntimeException("부재 정보 조회 중 서비스 연결 오류");
    }

    @Override
    public Page<ApprovalRequestResponseDto> getPendingAbsences(int page, int size) {
        log.error("AbsenceService getPendingAbsences 호출 실패");
        throw new RuntimeException("대기 중인 부재 목록 조회 중 서비스 연결 오류");
    }

    @Override
    public Page<ApprovalRequestResponseDto> getProcessedAbsences(int page, int size) {
        log.error("AbsenceService getProcessedAbsences 호출 실패");
        throw new RuntimeException("처리된 부재 목록 조회 중 서비스 연결 오류");
    }

    @Override
    public Page<ApprovalRequestResponseDto> getMyAbsences(Long userId, int page, int size) {
        log.error("AbsenceService getMyAbsences 호출 실패 - userId: {}", userId);
        throw new RuntimeException("내 부재 목록 조회 중 서비스 연결 오류");
    }

    @Override
    public Page<ApprovalRequestResponseDto> getAbsencesProcessedByMe(Long approverId, int page, int size) {
        log.error("AbsenceService getAbsencesProcessedByMe 호출 실패 - approverId: {}", approverId);
        throw new RuntimeException("내가 처리한 부재 목록 조회 중 서비스 연결 오류");
    }

    @Override
    public AbsenceApprovalStatisticsDto getAbsenceStatistics() {
        log.error("AbsenceService getAbsenceStatistics 호출 실패");
        throw new RuntimeException("부재 통계 조회 중 서비스 연결 오류");
    }
}