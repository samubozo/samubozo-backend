package com.playdata.messageservice.client;

import com.playdata.messageservice.dto.UserFeignResDto; // 이 DTO는 hr-service의 UserFeignResDto와 동일해야 합니다.
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "hr-service") // hr-service의 Eureka 서비스 이름
public interface HrServiceClient {

    @GetMapping("/hr/user/feign/{email}")
    UserFeignResDto getUserByEmail(@PathVariable String email);

    @GetMapping("/hr/user/feign/userName/{userName}")
    List<UserFeignResDto> getUserByUserName(@PathVariable("userName") String userName);

    // 필요하다면 employeeNo로 조회하는 엔드포인트도 추가
    // @GetMapping("/hr/user/feign/employeeNo/{employeeNo}")
    // UserFeignResDto getUserByEmployeeNo(@PathVariable Long employeeNo);
}