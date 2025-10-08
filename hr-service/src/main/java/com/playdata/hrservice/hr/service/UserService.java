package com.playdata.hrservice.hr.service;

import com.playdata.hrservice.hr.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

public interface UserService {

    UserResDto createUser(UserSaveReqDto dto, String hrRole);

    String uploadProfile(UserRequestDto dto) throws Exception;

    void updateUser(Long employeeNo, UserUpdateRequestDto dto, String hrRole) throws Exception;

    UserResDto getUserByEmployeeNo(Long employeeNo);

    UserLoginFeignResDto getUserByEmail(String email);

    UserFeignResDto getEmployeeByEmployeeNo(Long employeeNo);

    List<UserFeignResDto> getEmployeeByUserName(String userName);

    UserFeignResDto getEmployeeByEmail(String email);

    UserFeignResDto getEmployeeById(Long employeeNo);

    void updatePassword(String userEmail, UserPwUpdateDto dto);

    Page<UserListResDto> listUsers(Pageable pageable, String hrRole);

    Page<UserListResDto> searchUsers(UserSearchCondition condition, Pageable pageable);

    void retireUser(Long employeeNo, String hrRole);

    boolean hasApprovedExternalSchedule(Long userId, LocalDate date);

    String getApprovedExternalScheduleType(Long userId, LocalDate date);

    List<UserResDto> getUsersByIds(List<Long> employeeNos);

    List<UserResDto> getUsersWithFirstAnniversaryInMonth(int year, int month);

}
