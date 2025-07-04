package com.playdata.hrservice.hr.controller;


import com.playdata.hrservice.common.auth.TokenUserInfo;
import com.playdata.hrservice.common.dto.CommonResDto;
import com.playdata.hrservice.hr.dto.*;
import com.playdata.hrservice.hr.entity.Position;
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
    @GetMapping("/user/{email}")
    public UserLoginFeignResDto getLoginUser(@PathVariable String email) {
        return userService.getUserByEmail(email);
    }

    @GetMapping("/users/{email}")
    public UserFeignResDto getUser(@PathVariable String email) {
        return userService.getEmloyeeByEmail(email);
    }

    @PostMapping("/hr/user/password")
    ResponseEntity<?> updatePassword(@RequestBody UserPwUpdateDto dto) {
        userService.updatePassword(dto);
        return new ResponseEntity<>(HttpStatus.OK);
    }

//    // 사용자 정보 수정
//    @PatchMapping("/users/{id}")
//    public ResponseEntity<?> updateUser(@PathVariable("id") Long employeeNo,
//                                        @RequestBody UserUpdateRequestDto dto,
//                                        @AuthenticationPrincipal TokenUserInfo tokenUserInfo) {
//        String hrRole = tokenUserInfo.getHrRole();
//        userService.updateUser(employeeNo, dto, hrRole);
//        return new ResponseEntity<>(HttpStatus.OK);
//    }

    // 직원 조회
    @GetMapping("/user/list")
    public ResponseEntity<?> listUsers(@PageableDefault(size = 5, sort = "employeeNo") Pageable pageable) {
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "Success", userService.listUsers(pageable)), HttpStatus.OK);
    }


//   밑에거는 참고용으로 남겨놔요. 쓸거 쓰시고 지우셔도 될듯
//    private final Set<String> usedCode = ConcurrentHashMap.newKeySet();
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
//
//    //카카오 콜백 요청 처리
//    @GetMapping("/kakao")
//    public void kakaoCallback(@RequestParam String code ,
//                              @RequestParam(required = false) String state, // state 파라미터 추가
//                              //응답을 평소처럼 주는게 아니라, 직접 커스텀 해서 클라이언트에게 전달
//                              HttpServletResponse response) throws IOException {
//        log.info("카카오 콜백 처리 시작! code: {}", code);
//        //기본 상태
//        String clientType = "user";
//
//
//        if (state != null && !state.isEmpty()) {
//            try {
//                // Base64 디코딩 (프론트에서 btoa로 인코딩했다면)
//                String decodedState = new String(Base64.getUrlDecoder().decode(state)); // URL-safe Base64 디코더 사용
//                ObjectMapper mapper = new ObjectMapper();
//                Map<String, String> stateMap = mapper.readValue(decodedState, Map.class);
//
//                String receivedClientType = stateMap.get("clientType");
//                if ("admin".equals(receivedClientType)) {
//                    clientType = "admin";
//                }
//                // TODO: CSRF 토큰 검증 로직 추가 (stateMap.get("csrfToken")과 서버에 저장된 토큰 비교)
//
//                log.info("클라이언트 타입: {}", clientType);
//
//            } catch (Exception e) {
//                log.error("state 파라미터 디코딩 및 파싱 에러: {}", e.getMessage());
//                // 에러 발생 시 기본값 유지 또는 에러 페이지로 리다이렉트
//            }
//        }
//
//
//
//        //인가 코드로 엑세스토큰받기
//        String kakaoAccessToken = userService.getKakaoAccessToken(code);
//        //엑세스 토큰으로 사용자 정보받기
//        KakaoUserDto dto = userService.getKakaoUserInfo(kakaoAccessToken);
//        // 회원가입 or 로그인 처리
//        UserResDto resDto = userService.findOrCreateKakaoUser(dto, clientType);
//
//        //JWT 토큰 생성( 우리 사이트 로그인 유지를 위해. 사용자 정보를 위해.)
//        String token = jwtTokenProvider.createToken(resDto.getEmail(), resDto.getRole().name());
//        String refreshToken = jwtTokenProvider.createRefreshToken(resDto.getEmail(), resDto.getRole().name());
//
//        //redis에 저장
//        redisTemplate.opsForValue().set("user:refresh:" + resDto.getUserid(), refreshToken, 2, TimeUnit.MINUTES);
//
//        String html = "";
//        //팝업 닫기
//        if(clientType.equals("user")) {
//            html = String.format("""
//                    <!DOCTYPE html>
//                    <html>
//                    <head><title>카카오 로그인 완료</title></head>
//                    <body>
//                        <script>
//                            if (window.opener) {
//                                window.opener.postMessage({
//                                    type: 'OAUTH_SUCCESS',
//                                    token: '%s',
//                                    id: '%s',
//                                    email: '%s',
//                                    role: '%s',
//                                    provider: 'KAKAO'
//                                }, 'https://say4team.shop');
//                                window.close();
//                            } else {
//                                window.location.href = 'https://say4team.shop';
//                            }
//                        </script>
//                        <p>카카오 로그인 처리 중...</p>
//                    </body>
//                    </html>
//                    """,
//                    token, resDto.getUserid(), resDto.getEmail(), resDto.getRole().toString());
//        }else {
//            html = String.format("""
//                    <!DOCTYPE html>
//                    <html>
//                    <head><title>카카오 로그인 완료</title></head>
//                    <body>
//                        <script>
//                            if (window.opener) {
//                                window.opener.postMessage({
//                                    type: 'OAUTH_SUCCESS',
//                                    token: '%s',
//                                    id: '%s',
//                                    email: '%s',
//                                    role: '%s',
//                                    provider: 'KAKAO'
//                                }, 'http://localhost:9090');
//                                window.close();
//                            } else {
//                                window.location.href = 'http://localhost:9090';
//                            }
//                        </script>
//                        <p>카카오 로그인 처리 중...</p>
//                    </body>
//                    </html>
//                    """,
//                    token, resDto.getUserid(), resDto.getEmail(), resDto.getRole().toString());
//        }
//        response.setContentType("text/html;charset=utf-8");
//        response.getWriter().write(html);
//    }
//
//
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








