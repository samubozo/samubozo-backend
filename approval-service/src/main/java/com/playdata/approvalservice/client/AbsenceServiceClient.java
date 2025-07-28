package com.playdata.approvalservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "attendance-service")
public interface AbsenceServiceClient {

    @PutMapping("/attendance/absence/{absenceId}/approve")
    void approveAbsence(
            @PathVariable("absenceId") Long absenceId,
            @RequestParam("approverId") Long approverId
    );

    @PutMapping("/attendance/absence/{absenceId}/reject")
    void rejectAbsence(
            @PathVariable("absenceId") Long absenceId,
            @RequestParam("approverId") Long approverId,
            @RequestParam("rejectComment") String rejectComment
    );

    @GetMapping("/attendance/absence/{absenceId}")
    Object getAbsenceById(@PathVariable("absenceId") Long absenceId);

    @GetMapping("/attendance/absence/pending")
    Object getPendingAbsences();

    @GetMapping("/attendance/absence/processed")
    Object getProcessedAbsences();

    @GetMapping("/attendance/absence/my")
    Object getMyAbsences(@RequestParam("userId") Long userId);

    @GetMapping("/attendance/absence/processed-by-me")
    Object getAbsencesProcessedByMe(@RequestParam("approverId") Long approverId);

    @GetMapping("/attendance/absence/statistics")
    Object getAbsenceStatistics();
}