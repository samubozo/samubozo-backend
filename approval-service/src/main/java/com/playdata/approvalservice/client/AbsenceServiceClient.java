package com.playdata.approvalservice.client;

import com.playdata.approvalservice.approval.dto.AbsenceApprovalStatisticsDto;
import com.playdata.approvalservice.approval.dto.ApprovalRequestResponseDto;
import com.playdata.approvalservice.client.AbsenceServiceClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "attendance-service", fallback = AbsenceServiceClientFallback.class)
public interface AbsenceServiceClient {

    @PostMapping("/attendance/absence/{absenceId}/approve")
    void approveAbsence(
            @PathVariable("absenceId") Long absenceId,
            @RequestParam("approverId") Long approverId
    );

    @PostMapping("/attendance/absence/{absenceId}/reject")
    void rejectAbsence(
            @PathVariable("absenceId") Long absenceId,
            @RequestParam("approverId") Long approverId,
            @RequestParam("rejectComment") String rejectComment
    );

    @GetMapping("/attendance/absence/{absenceId}")
    ApprovalRequestResponseDto getAbsenceById(@PathVariable("absenceId") Long absenceId);

    @GetMapping("/attendance/absence/pending")
    Page<ApprovalRequestResponseDto> getPendingAbsences(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );

    @GetMapping("/attendance/absence/processed")
    Page<ApprovalRequestResponseDto> getProcessedAbsences(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );

    @GetMapping("/attendance/absence/my")
    Page<ApprovalRequestResponseDto> getMyAbsences(
            @RequestParam("userId") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );

    @GetMapping("/attendance/absence/processed-by-me")
    Page<ApprovalRequestResponseDto> getAbsencesProcessedByMe(
            @RequestParam("approverId") Long approverId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );

    @GetMapping("/attendance/absence/statistics")
    AbsenceApprovalStatisticsDto getAbsenceStatistics();

}