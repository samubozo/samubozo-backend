package com.playdata.payrollservice.client;

import com.playdata.payrollservice.common.configs.FeignClientConfig;
import com.playdata.payrollservice.common.dto.CommonResDto;
import com.playdata.payrollservice.payroll.dto.AttendanceResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "attendance-service")
public interface AttendanceClient {

    @GetMapping("/attendance/monthly/{year}/{month}")
    CommonResDto<List<AttendanceResDto>> getMonthlyAttendanceForFeign(
            @RequestParam("userId") Long userId,
            @PathVariable("year") int year,
            @PathVariable("month") int month
    );
}


