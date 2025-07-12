package com.playdata.scheduleservice.client;

import com.playdata.scheduleservice.dto.UserFeignResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "hr-service")
public interface HrServiceClient {

    @GetMapping("/hr/user/feign/employeeNo/{employeeNo}")
    UserFeignResDto getUserByEmployeeNo(@PathVariable("employeeNo") Long employeeNo);
}