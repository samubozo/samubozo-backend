package com.playdata.hrservice.hr.controller;


import com.playdata.hrservice.common.auth.TokenUserInfo;
import com.playdata.hrservice.common.dto.CommonResDto;
import com.playdata.hrservice.hr.dto.*;
import com.playdata.hrservice.hr.entity.Position;
import com.playdata.hrservice.hr.service.DepartmentService;
import com.playdata.hrservice.hr.service.PositionService;
import com.playdata.hrservice.hr.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/hr")
@RequiredArgsConstructor
@Slf4j
@RefreshScope // spring cloud config가 관리하는 파일의 데이터가 변경되면 빈들을 새로고침해주는 어노테이션
public class HRController {

    private final UserService userService;
    private final DepartmentService departmentService;
    private final PositionService positionService;
    private final RedisTemplate<String, Object> redisTemplate;

    private final Environment env;

    // 직원 계정 생성(등록)
    @PostMapping("/users/signup")
    public ResponseEntity<?> createUser(@Valid @RequestBody UserSaveReqDto dto) {
        UserResDto saved = userService.createUser(dto);
        CommonResDto resDto = new CommonResDto(HttpStatus.CREATED, "User created", saved);
        return new ResponseEntity<>(resDto, HttpStatus.CREATED);
    }

    // 프로필
    @PostMapping("/user/profile")
    public ResponseEntity<?> uploadProfile(@ModelAttribute UserRequestDto dto) throws Exception{
        String newProfile = userService.uploadProfile(dto);
        CommonResDto resDto = new CommonResDto(HttpStatus.OK,
                "User profile created", Map.of("newProfileName", newProfile));
        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }

    // feign client 요청을 위한 메서드
    // 이메일로 유저 정보 얻어오기
    // 로그인 용으로 간략 정보 얻을 때 쓰기
    @GetMapping("/user/feign/{email}")
    public UserLoginFeignResDto getLoginUser(@PathVariable String email) {
        return userService.getUserByEmail(email);
    }

    // 인증되어 권한있는 사람이 요청할 수 있는 상세 정보 조회 API
    @GetMapping("/users/detail")
    public ResponseEntity<CommonResDto> getMyUserInfo(@AuthenticationPrincipal TokenUserInfo tokenUserInfo) {
        String email = tokenUserInfo.getEmail();
        UserFeignResDto user = userService.getEmployeeByEmail(email);
        CommonResDto resDto = new CommonResDto(HttpStatus.OK, "User info retrieved successfully", user);
        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }

    // 인증된 사용자가 employeeNo로 상세정보 요청할 수 있는 API
    @GetMapping("/users/feign/{employeeNo}")
    public ResponseEntity<CommonResDto> getUserById(@PathVariable Long employeeNo) {
        UserFeignResDto user = userService.getEmployeeById(employeeNo);
        if (user == null) {
            return new ResponseEntity<>(new CommonResDto(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.", null), HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "User info retrieved successfully", user), HttpStatus.OK);
    }

    @PostMapping("/hr/user/password")
    ResponseEntity<?> updatePassword(@RequestBody UserPwUpdateDto dto) {
        userService.updatePassword(dto);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // 사용자 정보 수정
    @PatchMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable("id") Long employeeNo,
                                        @RequestBody UserUpdateRequestDto dto,
                                        @AuthenticationPrincipal TokenUserInfo tokenUserInfo) throws Exception {
        String hrRole = tokenUserInfo.getHrRole();
        userService.updateUser(employeeNo, dto, hrRole);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    // 직원 리스트 조회
    // 직원 조회 (기존 listUsers)
    @GetMapping("/user/list")
    public ResponseEntity<?> listUsers(@PageableDefault(size = 10, sort = "employeeNo")Pageable pageable,
                                       @AuthenticationPrincipal TokenUserInfo tokenUserInfo) {
        String hrRole = tokenUserInfo.getHrRole();
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "Success",
                userService.listUsers(pageable, hrRole)), HttpStatus.OK);
    }

    // 사용자 검색 엔드포인트 수정
    @GetMapping("/users/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam(required = false) String userName,
            @RequestParam(required = false) String departmentName,
            @PageableDefault(size = 10, sort = "employeeNo") Pageable pageable) {
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "Success", userService.searchUsers(userName, departmentName, pageable)), HttpStatus.OK);
    }

    // 부서 정보 조회 API
    @GetMapping("/departments")
    public ResponseEntity<?> getAllDepartments() {
        List<DepartmentResDto> departments = departmentService.getAllDepartments();
        CommonResDto resDto = new CommonResDto(HttpStatus.OK, "Departments retrieved successfully", departments);
        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }

    // 부서 추가
    @PostMapping("/departments")
    public ResponseEntity<?> createDepartment(@RequestBody DepartmentReqDto dto) {
        log.info("Create department : {}", dto);
        departmentService.createDepartment(dto);
        return ResponseEntity.ok().build();
    }

    // 직책 정보 조회 API
    @GetMapping("/positions")
    public ResponseEntity<?> getAllPositions() {
        List<PositionResDto> positions = positionService.getAllPositions();
        CommonResDto resDto = new CommonResDto(HttpStatus.OK, "Positions retrieved successfully", positions);
        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }

    // 직원 상세 조회
    @GetMapping("/user/{id}")
    public ResponseEntity<?> getUserDetail(@PathVariable("id") Long employeeNo) {
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "Success", userService.getUserByEmployeeNo(employeeNo)), HttpStatus.OK);
    }

    // 직원 퇴사 처리
    @PatchMapping("/users/retire/{id}")
    public ResponseEntity<?> retireUser(@PathVariable("id") Long employeeNo,
                                        @AuthenticationPrincipal TokenUserInfo tokenUserInfo) {
        String hrRole = tokenUserInfo.getHrRole();
        userService.retireUser(employeeNo, hrRole);
        return new ResponseEntity<>(HttpStatus.OK);
    }

}








