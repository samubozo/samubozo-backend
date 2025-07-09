package com.playdata.payrollservice.payroll.repository;

import com.playdata.payrollservice.payroll.dto.UserResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    @GetMapping("hr/users/{userId}")
    UserResDto getUserById(@PathVariable("userId") Long userId);
}
