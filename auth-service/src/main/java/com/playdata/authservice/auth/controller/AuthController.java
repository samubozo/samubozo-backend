package com.playdata.authservice.auth.controller;


import com.playdata.authservice.auth.dto.*;
import com.playdata.authservice.auth.service.AuthService;
import com.playdata.authservice.common.auth.JwtTokenProvider;
import com.playdata.authservice.common.auth.TokenRefreshRequestDto;
import com.playdata.authservice.common.auth.TokenUserInfo;
import com.playdata.authservice.common.dto.CommonResDto;
import jakarta.validation.Valid;
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
        UserLoginFeignResDto user = authService.login(dto);

        String token
                = jwtTokenProvider.createToken(user.getEmail(), user.getHrRole());
        String refreshToken
                = jwtTokenProvider.createRefreshToken(user.getEmail(), user.getHrRole());

        redisTemplate.opsForValue().set("user:refresh:" + user.getEmployeeNo(), refreshToken, 7, TimeUnit.MINUTES);

        Map<String, Object> loginInfo = new HashMap<>();
        loginInfo.put("token", token);
        loginInfo.put("employeeNo", user.getEmployeeNo());
        loginInfo.put("user_name", user.getUsername());
        loginInfo.put("hrRole", user.getHrRole());

        CommonResDto resDto
                = new CommonResDto(HttpStatus.OK,
                "Login Success", loginInfo);
        return new ResponseEntity<>(resDto, HttpStatus.OK);
    }

    // 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody TokenRefreshRequestDto requestDto) {
        try {
            TokenUserInfo userInfo = jwtTokenProvider.validateAndGetTokenUserInfo(requestDto.getRefreshToken());
            String savedToken = (String) redisTemplate.opsForValue().get(userInfo.getEmail());

            if (!requestDto.getRefreshToken().equals(savedToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("Refresh Token mismatch");
            }

            String newAccessToken = jwtTokenProvider.createToken(userInfo.getEmail(), userInfo.getHrRole());

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
    public ResponseEntity<Void> sendVerificationCode(@Valid @RequestBody FindPwDto dto) {
        authService.sendPasswordResetCode(dto.getEmail());
        return ResponseEntity.ok().build();
    }

    // 인증코드 검증
    @PostMapping("/verify-code")
    public ResponseEntity<CommonResDto> verifyCode(@Valid @RequestBody VerifyCodeDto dto) {
        try {
            authService.verifyResetCode(dto.getEmail(), dto.getCode());
            return ResponseEntity.ok(
                    new CommonResDto(
                            HttpStatus.OK,
                            "인증 코드가 일치합니다.",
                            null
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(new CommonResDto(
                            HttpStatus.BAD_REQUEST,
                            e.getMessage(),
                            null
                    ));
        }
    }

    // 비밀번호 재설정
    @PostMapping("/reset-password")
    public ResponseEntity<CommonResDto> resetPassword(@Valid @RequestBody ResetPasswordDto dto) {
        try {
            authService.resetPassword(dto.getEmail(), dto.getCode(), dto.getNewPassword());
            return ResponseEntity.ok(
                    new CommonResDto(
                            HttpStatus.OK,
                            "비밀번호 재설정이 완료되었습니다.",
                            null
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(new CommonResDto(
                            HttpStatus.BAD_REQUEST,
                            e.getMessage(),
                            null
                    ));
        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResDto(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "서버 에러가 발생했습니다. 다시 시도해주세요.",
                            null
                    ));
        }
    }

    // 유요한 이메일인지 검증 요청
    @PostMapping("/email-valid")
    public ResponseEntity<?> emailValid(@RequestBody Map<String, String> map) {
        String email = map.get("email");
        log.info("이메일 인증 요청! email: {}", email);
        try {
            String authNum = authService.mailCheck(email);
            // 성공: 200 + 인증번호
            return ResponseEntity.ok(
                    new CommonResDto(
                            HttpStatus.OK,
                            "인증 코드 발송 성공",
                            authNum
                    )
            );
        } catch (IllegalArgumentException e) {
            // 중복 이메일 또는 차단 상태
            return ResponseEntity
                    .badRequest()
                    .body(new CommonResDto(
                            HttpStatus.BAD_REQUEST,
                            e.getMessage(),
                            null
                    ));
        } catch (RuntimeException e) {
            // 메일 전송 실패 등
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new CommonResDto(
                            HttpStatus.INTERNAL_SERVER_ERROR,
                            "이메일 전송 과정 중 문제 발생!",
                            null
                    ));
        }
    }

    // 인증코드 검증 요청
    @PostMapping("/verify")
    public ResponseEntity<?> verifyEmail(@RequestBody Map<String, String> map) {
        log.info("인증 코드 검증! map: {}", map);
        Map<String, String> result = authService.verifyEmail(map);
        return ResponseEntity.ok().body("인증 성공!");
    }

}








