package com.playdata.certificateservice.client;


import com.playdata.certificateservice.dto.UserFeignResDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


//여기에 요청보낼 타 서비스 이름을 쓰세요. 쿠버내티스는 url 까지 써서 지정해야한데유
@FeignClient(name = "hr-service")
public interface HrServiceClient {

    @GetMapping("/hr/users/feign/{employeeNo}")
    UserFeignResDto getUserById(@PathVariable Long employeeNo);

}
