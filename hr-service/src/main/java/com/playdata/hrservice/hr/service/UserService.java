package com.playdata.hrservice.hr.service;

import com.playdata.hrservice.hr.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface UserService {

    @Transactional
    UserResDto createUser(UserSaveReqDto dto, String hrRole);

    String uploadProfile(UserRequestDto dto) throws Exception;

    @Transactional
    void updateUser(Long employeeNo, UserUpdateRequestDto dto, String hrRole) throws Exception;

    UserResDto getUserByEmployeeNo(Long employeeNo);

    UserLoginFeignResDto getUserByEmail(String email);

    @Transactional(readOnly = true)
    UserFeignResDto getEmployeeByEmployeeNo(Long employeeNo);

    List<UserFeignResDto> getEmployeeByUserName(String userName);

    UserFeignResDto getEmployeeByEmail(String email);

    UserFeignResDto getEmployeeById(Long employeeNo);

    void updatePassword(UserPwUpdateDto dto);

    Page<UserResDto> listUsers(Pageable pageable, String hrRole);

    Object searchUsers(String userName, String departmentName, String hrRole, Pageable pageable);

    void retireUser(Long employeeNo, String hrRole);

    boolean hasApprovedExternalSchedule(Long userId, LocalDate date);

    String getApprovedExternalScheduleType(Long userId, LocalDate date);

    @Transactional(readOnly = true)
    List<UserResDto> getUsersByIds(List<Long> employeeNos);

    @Transactional(readOnly = true)
    List<UserResDto> getUsersWithFirstAnniversaryInMonth(int year, int month);

}
