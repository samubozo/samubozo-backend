package com.playdata.authservice.auth.controller;


import com.playdata.authservice.auth.dto.UserLoginReqDto;
import com.playdata.authservice.auth.entity.User;
import com.playdata.authservice.auth.service.AuthService;
import com.playdata.authservice.common.auth.JwtTokenProvider;
import com.playdata.authservice.common.auth.TokenRefreshRequestDto;
import com.playdata.authservice.common.auth.TokenUserInfo;
import com.playdata.authservice.common.dto.CommonResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@RefreshScope // spring cloud config가 관리하는 파일의 데이터가 변경되면 빈들을 새로고침해주는 어노테이션
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    private final Environment env;

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserLoginReqDto dto) {
        User user = authService.login(dto);

        String token
                = jwtTokenProvider.createToken(user.getEmail(), user.getRoleId().toString());
        String refreshToken
                = jwtTokenProvider.createRefreshToken(user.getEmail(), user.getRoleId().toString());

        redisTemplate.opsForValue().set("user:refresh:" + user.getEmployeeNo(), refreshToken, 7, TimeUnit.MINUTES);

        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("token", token);
        loginInfo.put("employeeNo", user.getEmployeeNo());
        loginInfo.put("user_name", user.getUserName());
        loginInfo.put("email", user.getEmail());
        loginInfo.put("phone", user.getPhone());
        loginInfo.put("address", user.getAddress());
        loginInfo.put("roleId", user.getRoleId().toString());

        CommonResDto resDto
                = new CommonResDto(HttpStatus.OK,
                "Login Success", loginInfo);
        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }

    // 토큰 재발급
    @PostMapping("/token/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody TokenRefreshRequestDto requestDto) {
        try {
            TokenUserInfo userInfo = jwtTokenProvider.validateAndGetTokenUserInfo(requestDto.getRefreshToken());
            String savedToken = (String) redisTemplate.opsForValue().get(userInfo.getEmail());

            if (!requestDto.getRefreshToken().equals(savedToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Refresh Token mismatch");
            }

            String newAccessToken = jwtTokenProvider.createToken(userInfo.getEmail(), userInfo.getRole().name());

            Map<String, String> tokenMap = new HashMap<>();
            tokenMap.put("accessToken", newAccessToken);
            return ResponseEntity.ok(tokenMap);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid Refresh Token: " + e.getMessage());
        }
    }

    // 비밀번호 찾기
    @PostMapping("/find-password")



//   밑에거는 참고용으로 남겨놔요. 쓸거 쓰시고 지우셔도 될듯
//    private final Set<String> usedCode = ConcurrentHashMap.newKeySet();
//
//    @PostMapping("/create")
//    public ResponseEntity<?> userCreate(@Valid @RequestBody UserSaveReqDto dto,
//                                        @RequestParam(required = false, defaultValue = "user") String role) {
//
//        dto.setRole(role);
//        User saved = userService.userCreate(dto);
//
//        CommonResDto resDto
//                = new CommonResDto(HttpStatus.CREATED,
//                "User Created", saved.getName());
//
//        return new ResponseEntity<>(resDto, HttpStatus.CREATED);
//    }
//
//    @PreAuthorize("hasRole('ADMIN')")
//    @GetMapping("/list")
//    public ResponseEntity<?> getUserList(Pageable pageable) {
//        List<UserResDto> dtoList = userService.userList(pageable);
//        CommonResDto resDto
//                = new CommonResDto(HttpStatus.OK, "userList 조회 성공", dtoList);
//
//        return ResponseEntity.ok().body(resDto);
//    }
//
//    @PreAuthorize("hasAuthority('ROLE_USER')")
//    @GetMapping("/profile/{id}")
//    public ResponseEntity<?> getProfile(@PathVariable("id") String  id) {
//        try {
//            Long userId = Long.parseLong(id);
//            User user = userService.findById(userId);
//            return ResponseEntity.ok().body(user);
//        } catch (NumberFormatException e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
//                    .body("ID 형식이 잘못되었습니다.");
//        }
//    }
//
//    @PutMapping("/update/{userId}")
//    public ResponseEntity<?> updateUser(
//            @PathVariable Long userId,
//            @RequestBody UserUpdateRequestDto dto
//    ) {
//        User updatedUser = userService.updateUser(userId, dto);
//
//        return ResponseEntity.ok().body(new CommonResDto(
//                HttpStatus.OK,
//                "회원정보가 수정되었습니다.",
//                updatedUser
//        ));
//    }
//
//    @PatchMapping("/address/{userId}")
//    public ResponseEntity<?> updateAddress(
//            @PathVariable Long userId,
//            @RequestBody UserAddressUpdateDto dto
//    ) {
//        User updatedUser = userService.updateUserAddress(userId, dto.getAddress());
//
//        return ResponseEntity.ok().body(new CommonResDto(
//                HttpStatus.OK,
//                "주소가 수정되었습니다.",
//                updatedUser
//        ));
//    }
//
//    @DeleteMapping("/delete/{userId}")
//    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
//        userService.deleteUser(userId);
//        return ResponseEntity.ok(new CommonResDto(
//                HttpStatus.OK, "회원 탈퇴 완료", Collections.singletonMap("deleted", true)
//        ));
//
//    }
//
//    @PutMapping("/restore/{userId}")
//    public ResponseEntity<?> restoreUser(@PathVariable Long userId) {
//        userService.restoreUser(userId);
//        return ResponseEntity.ok(new CommonResDto(
//                HttpStatus.OK, "회원 복구 완료", null
//        ));
//    }
//
//
//    @PostMapping("/token/refresh")
//    public ResponseEntity<?> refreshToken(@RequestBody TokenRefreshRequestDto requestDto) {
//        try {
//            TokenUserInfo userInfo = jwtTokenProvider.validateAndGetTokenUserInfo(requestDto.getRefreshToken());
//            String savedToken = (String) redisTemplate.opsForValue().get(userInfo.getEmail());
//
//            if (!requestDto.getRefreshToken().equals(savedToken)) {
//                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                        .body("Refresh Token mismatch");
//            }
//
//            String newAccessToken = jwtTokenProvider.createToken(userInfo.getEmail(), userInfo.getRole().name());
//
//            Map<String, String> tokenMap = new HashMap<>();
//            tokenMap.put("accessToken", newAccessToken);
//            return ResponseEntity.ok(tokenMap);
//
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//                    .body("Invalid Refresh Token: " + e.getMessage());
//        }
//    }
//    @GetMapping("/findByEmail")
//    public ResponseEntity<?> getUserByEmail(@RequestParam String email) {
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
//
//        log.info("getUserByEmail: email: {}", email);
//        UserResDto dto = userService.findByEmail(email);
//        CommonResDto resDto
//                = new CommonResDto(HttpStatus.OK, "이메일로 회원 조회 완료", dto);
//        return ResponseEntity.ok().body(resDto);
//    }
//
//    @PostMapping("/email-valid")
//    public ResponseEntity<?> emailVaild(@RequestBody Map<String, String> map) {
//        String email = map.get("email");
//        log.info("이메일 인증 요청!: {}", map.get("email"));
//        String authNum = userService.mailCheck(email);
//        return ResponseEntity.ok().body(authNum);
//    }
//
//    //인증코드 검증 요청
//    @PostMapping("/verify")
//    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> map) {
//        log.info("인증코드검증! map:{}",map);
//        Map<String, String> result = userService.verifyEmail(map);
//        return ResponseEntity.ok().body("인증 성공!");
//
//    }

//    @GetMapping("/health-check")
//    public String healthCheck() {
//        String msg = "It's Working in User-service!\n";
//        msg += "token.expiration_time: " + env.getProperty("token.expiration_time");
//        msg += "token.secret: " + env.getProperty("token.secret");
//        msg += "aws.accessKey: " + env.getProperty("aws.accessKey");
//        msg += "aws.secretKey: " + env.getProperty("aws.secretKey");
//        msg += "message: " + env.getProperty("message");
//
//
//        return msg;
//    }
//
//

}








