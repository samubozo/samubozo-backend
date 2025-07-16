package com.playdata.approvalservice.client;

import com.playdata.approvalservice.client.dto.UserResDto;
import com.playdata.approvalservice.common.configs.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "hr-service", configuration = FeignClientConfig.class)
public interface HrServiceClient {

    @GetMapping("/hr/users")
    List<UserResDto> getUsersInfo(@RequestParam("userIds") List<Long> userIds);
}
