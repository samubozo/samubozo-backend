package com.playdata.vacationservice.client;

import com.playdata.vacationservice.client.dto.WorkStatusCreateRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "attendance-service")
public interface AttendanceServiceClient {

    @PostMapping("/attendances/internal/work-status/vacation")
    void createWorkStatusForVacation(@RequestBody WorkStatusCreateRequestDto requestDto);
}
