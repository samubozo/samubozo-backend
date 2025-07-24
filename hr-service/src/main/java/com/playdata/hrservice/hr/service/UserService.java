package com.playdata.hrservice.hr.service;

import com.playdata.hrservice.hr.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface UserService {
    // 회원가입
    @Transactional
    UserResDto createUser(UserSaveReqDto dto, String hrRole);

    // 프로필
    String uploadProfile(UserRequestDto dto) throws Exception;

    // 사용자 정보 수정
    @Transactional
    void updateUser(Long employeeNo, UserUpdateRequestDto dto, String hrRole) throws Exception;

    // 직원 상세 조회
    UserResDto getUserByEmployeeNo(Long employeeNo);

    // 로그인을 위한 Feign
    UserLoginFeignResDto getUserByEmail(String email);

    // Feign client용: employeeNo로 사용자 정보 조회
    @Transactional(readOnly = true)
    UserFeignResDto getEmployeeByEmployeeNo(Long employeeNo);

    // Feign client용: userName으로 사용자 정보 조회
    List<UserFeignResDto> getEmployeeByUserName(String userName);

    // 모든 서비스를 위한 Feign
    UserFeignResDto getEmployeeByEmail(String email);

    // 모든 서비스를 위한 Feign (id로 조회)
    UserFeignResDto getEmployeeById(Long employeeNo);

    // 비밀번호를 위한 Feign
    void updatePassword(UserPwUpdateDto dto);

    // 직원 조회
    Page<UserResDto> listUsers(Pageable pageable, String hrRole);

    // 사용자 검색 (조건에 따라 페이징 또는 전체 리스트 반환)
    Object searchUsers(String userName, String departmentName, String hrRole, Pageable pageable);

    // 퇴사 처리
    void retireUser(Long employeeNo, String hrRole);

    // 특정 사용자가 특정 날짜에 승인된 외부 일정(출장, 연수 등)이 있는지 확인
    boolean hasApprovedExternalSchedule(Long userId, LocalDate date);

    // 특정 사용자가 특정 날짜에 승인된 외부 일정의 종류를 조회
    String getApprovedExternalScheduleType(Long userId, LocalDate date);

    @Transactional(readOnly = true)
    List<UserResDto> getUsersByIds(List<Long> employeeNos);

    /**
     * 특정 연도와 월에 입사 1주년을 맞이하는 사용자 목록을 조회합니다.
     *
     * @param year 조회할 연도 (입사일 기준)
     * @param month 조회할 월 (입사일 기준)
     * @return 해당 월에 입사 1주년을 맞이하는 사용자 정보 DTO 목록
     */
    @Transactional(readOnly = true)
    List<UserResDto> getUsersWithFirstAnniversaryInMonth(int year, int month);
}
