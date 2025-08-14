package com.playdata.attendanceservice.client;

import com.playdata.attendanceservice.client.dto.UserDetailDto;
import com.playdata.attendanceservice.client.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

/**
 * HR 서비스와 통신하기 위한 Feign 클라이언트 인터페이스입니다.
 */
@FeignClient(name = "hr-service", configuration = com.playdata.attendanceservice.common.configs.FeignClientConfig.class)
public interface HrServiceClient {

    /**
     * 지정된 날짜에 입사 1주년을 맞이하는 모든 사용자의 정보를 조회합니다。
     * HR 서비스의 /api/v1/hr/anniversary 엔드포인트를 호출합니다。
     *
     * @param date 조회할 날짜 (YYYY-MM-DD 형식)
     * @return 입사 1주년을 맞이하는 사용자 정보 리스트
     */
    @GetMapping("/api/v1/hr/anniversary")
    List<UserDto> getUsersWithFirstAnniversary(@RequestParam("date") String date);

    /**
     * 특정 사용자의 상세 정보를 조회합니다.
     * HR 서비스의 /hr/users/{id} 엔드포인트를 호출합니다.
     *
     * @param id 조회할 사용자의 ID
     * @return 사용자의 상세 정보 (이름, 부서 등)
     */
    @GetMapping("/user/feign/employeeNo/{id}")
    UserDetailDto getUserDetails(@PathVariable("id") Long id);

    /**
     * 모든 사용자의 ID를 조회합니다.
     * HR 서비스의 /api/v1/hr/users/all-ids 엔드포인트를 호출합니다.
     *
     * @return 모든 사용자의 ID 리스트
     */
    @GetMapping("/api/v1/hr/users/all-ids")
    List<UserDto> getAllUserIds();

    @GetMapping("/hr/schedules/approved-type")
    String getApprovedExternalScheduleType(@RequestParam("userId") Long userId, @RequestParam("date") String date);

    /**
     * 특정 연도와 월에 입사 1주년을 맞이하는 사용자 목록을 조회합니다.
     * HR 서비스의 /hr/anniversary/monthly 엔드포인트를 호출합니다.
     *
     * @param year 조회할 연도 (입사일 기준)
     * @param month 조회할 월 (입사일 기준)
     * @return 해당 월에 입사 1주년을 맞이하는 사용자 정보 DTO 목록
     */
    @GetMapping("/hr/anniversary/monthly")
    List<UserDto> getUsersWithFirstAnniversaryInMonth(
            @RequestParam("year") int year,
            @RequestParam("month") int month);
}