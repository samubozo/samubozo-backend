package com.playdata.payrollservice.client;

import com.playdata.payrollservice.common.dto.CommonResDto;
import com.playdata.payrollservice.payroll.dto.UserResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "hr-service")
public interface HrClient {

    @GetMapping("/hr/user/{id}")
    CommonResDto<UserResDto> getUserById(@PathVariable("id") Long employeeNo);
}
