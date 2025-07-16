package com.playdata.vacationservice.client;

import com.playdata.vacationservice.client.dto.UserResDto;
import com.playdata.vacationservice.client.dto.UserResDto;
import com.playdata.vacationservice.client.dto.UserDetailDto;
import com.playdata.vacationservice.common.configs.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * HR 서비스와 통신하기 위한 Feign 클라이언트 인터페이스입니다.
 */
@FeignClient(name = "hr-service", configuration = FeignClientConfig.class)
public interface HrServiceClient {

    /**
     * 현재 인증된 사용자의 상세 정보를 HR 서비스로부터 조회합니다.
     * HR 서비스의 /hr/users/detail 엔드포인트를 호출합니다。
     * 이 API는 @AuthenticationPrincipal을 통해 사용자 정보를 받으므로,
     * Feign Client에서는 별도의 파라미터 없이 호출하고 FeignClientInterceptor를 통해 헤더로 인증 정보가 전달됩니다.
     *
     * @return 사용자의 상세 정보가 담긴 UserDetailDto
     */
    @GetMapping("/hr/users/detail")
    UserDetailDto getMyUserInfo();

    /**
     * HR 서비스로부터 여러 사용자의 정보를 조회합니다.
     *
     * @param userIds 조회할 사용자 ID 목록
     * @return 사용자 정보 DTO 목록
     */
    @GetMapping("/hr/users")
    List<UserResDto> getUsersInfo(@RequestParam("userIds") List<Long> userIds);
}
