package com.playdata.approvalservice.client;

import com.playdata.approvalservice.common.configs.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;

@FeignClient(name = "vacation-service", configuration = FeignClientConfig.class)
public interface VacationServiceClient {

    @PostMapping("/vacations/internal/update-balance-on-approval")
    void updateVacationBalanceOnApproval(
            @RequestParam("vacationId") Long vacationId,
            @RequestParam("status") String status,
            @RequestParam("userId") Long userId,
            @RequestParam("vacationType") String vacationType,
            @RequestParam("startDate") String startDate,
            @RequestParam("endDate") String endDate,
            @RequestParam(value = "rejectComment", required = false) String rejectComment);
}