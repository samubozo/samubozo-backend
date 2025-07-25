package com.playdata.certificateservice.client.hr;

import com.playdata.certificateservice.client.hr.dto.UserFeignResDto;
import com.playdata.certificateservice.common.dto.CommonResDto;
import com.playdata.certificateservice.client.hr.dto.UserResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "hr-service")
public interface HrServiceClient {

    @GetMapping("/hr/users/feign/{employeeNo}")
    CommonResDto<UserFeignResDto> getUserById(@PathVariable("employeeNo") Long employeeNo);

    @GetMapping("/hr/users")
    List<UserResDto> getUsersInfo(@RequestParam("userIds") List<Long> userIds);
}
