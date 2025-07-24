package com.playdata.hrservice.hr.controller;

import com.playdata.hrservice.common.auth.TokenUserInfo;
import com.playdata.hrservice.common.dto.CommonResDto;
import com.playdata.hrservice.hr.dto.*;
import com.playdata.hrservice.hr.service.DepartmentService;
import com.playdata.hrservice.hr.service.PositionService;
import com.playdata.hrservice.hr.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/hr")
@RequiredArgsConstructor
@Slf4j
@RefreshScope
public class HRController {

    private final UserService userService;
    private final DepartmentService departmentService;
    private final PositionService positionService;


    @PostMapping("/users/signup")
    public ResponseEntity<?> createUser(@Valid @RequestBody UserSaveReqDto dto, @AuthenticationPrincipal TokenUserInfo tokenUserInfo) {
        String hrRole = tokenUserInfo.getHrRole();
        UserResDto saved = userService.createUser(dto, hrRole);
        CommonResDto<UserResDto> resDto = new CommonResDto<>(HttpStatus.CREATED, "User created", saved);
        return new ResponseEntity<>(resDto, HttpStatus.CREATED);
    }

    @PostMapping("/user/profile")
    public ResponseEntity<?> uploadProfile(@ModelAttribute UserRequestDto dto) throws Exception{
        String newProfile = userService.uploadProfile(dto);
        CommonResDto<Map<String, String>> resDto = new CommonResDto<>(HttpStatus.OK,
                "User profile created", Map.of("newProfileName", newProfile));
        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }

    @GetMapping("/user/feign/{email}")
    public UserLoginFeignResDto getLoginUser(@PathVariable String email) {
        return userService.getUserByEmail(email);
    }

    @GetMapping("/users/detail")
    public ResponseEntity<CommonResDto<UserFeignResDto>> getMyUserInfo(@AuthenticationPrincipal TokenUserInfo tokenUserInfo) {
        String email = tokenUserInfo.getEmail();
        UserFeignResDto user = userService.getEmployeeByEmail(email);
        CommonResDto<UserFeignResDto> resDto = new CommonResDto<>(HttpStatus.OK,
                "User info retrieved successfully", user);
        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }

    @GetMapping("/users/feign/{employeeNo}")
    public ResponseEntity<CommonResDto<UserFeignResDto>> getUserById(@PathVariable Long employeeNo) {
        UserFeignResDto user = userService.getEmployeeById(employeeNo);
        if (user == null) {
            return new ResponseEntity<>(new CommonResDto<>(HttpStatus.NOT_FOUND,
                    "사용자를 찾을 수 없습니다.", null), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(new CommonResDto<>(HttpStatus.OK,
                "User info retrieved successfully", user), HttpStatus.OK);
    }

    @PostMapping("/user/password")
    ResponseEntity<?> updatePassword(@RequestBody UserPwUpdateDto dto) {
        userService.updatePassword(dto);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping(value = "/users/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateUser(
            @PathVariable("id") Long employeeNo,
            @ModelAttribute UserUpdateRequestDto dto,
            @AuthenticationPrincipal TokenUserInfo tokenUserInfo) throws Exception {
        String hrRole = tokenUserInfo.getHrRole();
        userService.updateUser(employeeNo, dto, hrRole);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/user/list")
    public ResponseEntity<?> listUsers(@PageableDefault(sort = "employeeNo")Pageable pageable,
                                       @AuthenticationPrincipal TokenUserInfo tokenUserInfo) {
        String hrRole = tokenUserInfo.getHrRole();
        return new ResponseEntity<>(new CommonResDto<>(HttpStatus.OK, "Success",
                userService.listUsers(pageable, hrRole)), HttpStatus.OK);
    }

    @GetMapping("/users/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String departmentName,
            @RequestParam(required = false) String hrRole,
            @PageableDefault(sort = "employeeNo") Pageable pageable) {
        Object result;
        if (userName != null || departmentName != null || hrRole != null) {
            result = userService.searchUsers(userName, departmentName, hrRole, null);
        } else {
            result = userService.searchUsers(null, null, null, pageable);
        }
        return new ResponseEntity<>(new CommonResDto<>(HttpStatus.OK,
                "Success", result), HttpStatus.OK);
    }

    @GetMapping("/departments")
    public ResponseEntity<?> getAllDepartments() {
        List<DepartmentResDto> departments = departmentService.getAllDepartments();
        CommonResDto<List<DepartmentResDto>> resDto = new CommonResDto<>(HttpStatus.OK,
                "Departments retrieved successfully", departments);
        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }

    @PostMapping(value = "/departments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createDepartment(@ModelAttribute DepartmentReqDto dto) {
        departmentService.createDepartment(dto);
        return ResponseEntity.ok().build();
    }

    @PutMapping(value = "/departments/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateDepartment(@PathVariable("id") Long departmentId, @ModelAttribute DepartmentReqDto dto) {
        departmentService.updateDepartment(departmentId, dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/departments/{id}")
    public ResponseEntity<?> deleteDepartment(@PathVariable("id") Long departmentId) {
        departmentService.deleteDepartment(departmentId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/positions")
    public ResponseEntity<?> getAllPositions() {
        List<PositionResDto> positions = positionService.getAllPositions();
        CommonResDto<List<PositionResDto>> resDto = new CommonResDto<>(HttpStatus.OK,
                "Positions retrieved successfully", positions);
        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }

    @GetMapping("/user/{id}")
    public ResponseEntity<?> getUserDetail(@PathVariable("id") Long employeeNo) {
        return new ResponseEntity<>(new CommonResDto<>(HttpStatus.OK,
                "Success", userService.getUserByEmployeeNo(employeeNo)), HttpStatus.OK);
    }

    @GetMapping("/user/feign/employeeNo/{employeeNo}")
    public UserFeignResDto getUserByEmployeeNo(@PathVariable Long employeeNo) {
        return userService.getEmployeeByEmployeeNo(employeeNo);
    }

    @GetMapping("/user/feign/userName/{userName}")
    public List<UserFeignResDto> getUserByUserName(@PathVariable String userName) {
        return userService.getEmployeeByUserName(userName);
    }

    @GetMapping("/users")
    public List<UserResDto> getUsersInfo(@RequestParam("userIds") List<Long> userIds) {
        return userService.getUsersByIds(userIds);
    }

    @GetMapping("/schedules/approved")
    public ResponseEntity<Boolean> hasApprovedExternalSchedule(
            @RequestParam("userId") Long userId,
            @RequestParam("date") LocalDate date) {
        boolean hasSchedule = userService.hasApprovedExternalSchedule(userId, date);
        return ResponseEntity.ok(hasSchedule);
    }

    @GetMapping("/schedules/approved-type")
    public ResponseEntity<String> getApprovedExternalScheduleType(
            @RequestParam("userId") Long userId,
            @RequestParam("date") LocalDate date) {
        String scheduleType = userService.getApprovedExternalScheduleType(userId, date);
        return ResponseEntity.ok(scheduleType);
    }

    @PatchMapping("/users/retire/{id}")
    public ResponseEntity<?> retireUser(@PathVariable("id") Long employeeNo,
                                        @AuthenticationPrincipal TokenUserInfo tokenUserInfo) {
        String hrRole = tokenUserInfo.getHrRole();
        userService.retireUser(employeeNo, hrRole);
        return new ResponseEntity<>(HttpStatus.OK);

    }

    @GetMapping("/anniversary/monthly")
    public ResponseEntity<CommonResDto<List<UserResDto>>> getUsersWithFirstAnniversaryInMonth(
            @RequestParam("year") int year,
            @RequestParam("month") int month) {
        List<UserResDto> users = userService.getUsersWithFirstAnniversaryInMonth(year, month);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK,
                "Success", users), HttpStatus.OK);
    }
}









