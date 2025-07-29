package com.playdata.attendanceservice.client;

import com.playdata.attendanceservice.attendance.absence.entity.Absence;
import com.playdata.attendanceservice.common.dto.CommonResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "approval-service")
public interface ApprovalServiceClient {

    /**
     * 부재 전자결재 요청을 생성합니다.
     * @param absence 부재 엔티티
     * @return 결재 요청 생성 결과
     */
    @PostMapping("/absence-requests")
    CommonResDto<Void> createAbsenceApprovalRequest(@RequestBody Absence absence);

    /**
     * 부재 전자결재를 승인합니다.
     * @param absenceId 부재 ID
     * @param approverId 결재자 ID
     * @return 승인 처리 결과
     */
    @PostMapping("/absence-requests/{absenceId}/approve")
    CommonResDto<Void> approveAbsenceApproval(@PathVariable Long absenceId, @RequestParam String approverId);

    /**
     * 부재 전자결재를 반려합니다.
     * @param absenceId 부재 ID
     * @param approverId 결재자 ID
     * @param rejectComment 반려 사유
     * @return 반려 처리 결과
     */
    @PostMapping("/absence-requests/{absenceId}/reject")
    CommonResDto<Void> rejectAbsenceApproval(@PathVariable Long absenceId,
                                             @RequestParam String approverId,
                                             @RequestParam String rejectComment);

    /**
     * 특정 사용자가 특정 날짜에 승인된 휴가가 있는지 확인합니다.
     * @param userId 확인할 사용자의 ID
     * @param date 확인할 날짜
     * @return 승인된 휴가가 있으면 true, 없으면 false
     */
    @GetMapping("/approvals/leaves/approved")
    boolean hasApprovedLeave(@RequestParam("userId") Long userId, @RequestParam("date") String date);

    /**
     * 특정 사용자가 특정 날짜에 승인된 휴가의 종류를 조회합니다.
     * @param userId 확인할 사용자의 ID
     * @param date 확인할 날짜
     * @return 승인된 휴가 종류
     */
    @GetMapping("/approvals/leaves/approved-type")
    String getApprovedLeaveType(@RequestParam("userId") Long userId, @RequestParam("date") String date);
}