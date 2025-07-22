package com.playdata.authservice.client;


import com.playdata.authservice.auth.dto.UserLoginFeignResDto;
import com.playdata.authservice.auth.dto.UserPwUpdateDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//여기에 요청보낼 타 서비스 이름을 쓰세요. 쿠버내티스는 url 까지 써서 지정해야한데유
@FeignClient(name = "hr-service")
public interface HrServiceClient {

    @GetMapping("/hr/user/feign/{email}")
    UserLoginFeignResDto getLoginUser(@PathVariable String email);

    @PostMapping("/hr/user/password")
    ResponseEntity<?> setPassword(@RequestBody UserPwUpdateDto dto);

}
