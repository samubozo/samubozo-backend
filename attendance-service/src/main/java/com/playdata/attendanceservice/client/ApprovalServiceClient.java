package com.playdata.attendanceservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "approval-service")
public interface ApprovalServiceClient {

    /**
     * 특정 사용자가 특정 날짜에 승인된 휴가(연차, 반차, 조퇴 등)가 있는지 확인합니다.
     * Approval 서비스의 API를 호출합니다.
     *
     * @param userId 확인할 사용자의 ID
     * @param date 확인할 날짜
     * @return 승인된 휴가가 있으면 true, 없으면 false
     */
    @GetMapping("/approvals/leaves/approved")
    boolean hasApprovedLeave(@RequestParam("userId") Long userId, @RequestParam("date") String date);

    /**
     * 특정 사용자가 특정 날짜에 승인된 휴가의 종류를 조회합니다.
     * Approval 서비스의 API를 호출합니다.
     *
     * @param userId 확인할 사용자의 ID
     * @param date 확인할 날짜
     * @return 승인된 휴가 종류 (예: "HALF_DAY_LEAVE", "ANNUAL_LEAVE"), 없으면 null
     */
    @GetMapping("/approvals/leaves/approved-type")
    String getApprovedLeaveType(@RequestParam("userId") Long userId, @RequestParam("date") String date);
}
