package com.playdata.scheduleservice.schedule.service;

import com.playdata.scheduleservice.common.auth.Role;
import com.playdata.scheduleservice.common.auth.TokenUserInfo;
import com.playdata.scheduleservice.schedule.dto.*;
import com.playdata.scheduleservice.schedule.entity.User;
import com.playdata.scheduleservice.schedule.entity.UserStatus;
import com.playdata.scheduleservice.schedule.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    //필요한 객체 생성하여 주입
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final RedisTemplate  redisTemplate;
    private final MailSenderService mailSenderService;

    // Redis Key 상수
    private static final String VERIFYCATION_CODE_KEY = "email_verify:code:";
    private static final String VERIFYCATION_ATTEMPT_KEY = "email_verify:attempt:";
    private static final String VERIFYCATION_BLOCK_KEY = "email_verify:block:";

    @Value("${oauth2.kakao.client-id}")
    private String kakaoClientId;
    @Value("${oauth2.kakao.redirect-uri}")
    private String redirectUri;


    public User userCreate(UserSaveReqDto dto) {
        Optional<User> foundEmail
                = userRepository.findByEmail(dto.getEmail());

        if (foundEmail.isPresent()) {

            throw new IllegalArgumentException("이미 존재하는 이메일 입니다!");
        }


        User user = dto.toEntity(encoder);
        User saved = userRepository.save(user);
        return saved;
    }

    public User login(UserLoginReqDto dto) {

        User user = userRepository.findByEmail(dto.getEmail()).orElseThrow(
                () -> new EntityNotFoundException("User not found!")
        );

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new IllegalArgumentException("비활성화된 계정입니다. (탈퇴 또는 정지)");
        }


        if (!encoder.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return user;
    }

    public UserResDto myInfo() {
        TokenUserInfo userInfo

                = (TokenUserInfo) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();

        User user = userRepository.findByEmail(userInfo.getEmail())
                .orElseThrow(
                        () -> new EntityNotFoundException("User not found!")
                );

        return user.fromEntity();
    }

    public User updateUser(Long userId, UserUpdateRequestDto dto) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new UsernameNotFoundException("회원 정보를 찾을 수 없습니다.")
        );
        if (!user.getEmail().equals(dto.getEmail())) {
            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("이미 사용중인 이메일 입니다.");
            }
            user.setEmail(dto.getEmail());
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            String encodedPassword = encoder.encode(dto.getPassword());
            user.setPassword(encodedPassword);
        }

        if (dto.getBirthDate() != null) {
            user.setBirthDate(dto.getBirthDate());
        }

        user.setName(dto.getName());
        user.setAddress(dto.getAddress());
        user.setPhone(dto.getPhone());

        return userRepository.save(user);
    }

    public User updateUserAddress(Long userId, String address) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setAddress(address);
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        log.info("탈퇴 실행 시작 userId={}", userId);
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("회원 없음"));

        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);

        log.info("탈퇴 완료: userId={}, status={}", userId, user.getStatus());
    }

    public void restoreUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("회원 없음"));

        if (user.getStatus() == UserStatus.ACTIVE) {
            throw new IllegalArgumentException("이미 활성화된 사용자입니다.");
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    public List<UserResDto> userList(Pageable pageable) {

        Page<User> users = userRepository.findAll(pageable);

        List<User> content = users.getContent();
        List<UserResDto> dtoList = content.stream()
                .map(User::fromEntity)
                .collect(Collectors.toList());

        return dtoList;

    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found!"));
    }

    public UserResDto findByEmail(String email) {
        User user = userRepository.findByEmail(email).orElseThrow(
                () -> new EntityNotFoundException("User not found!")
        );
        return user.fromEntity();
    }

    public String mailCheck(String email) {

        // 차단 상태 확인
        if(isBlocked(email)){
            throw new IllegalArgumentException("잘못된 요청 횟수가 과다하여 임시 차단 중입니다. 잠시 후에 시도해주세요.");
        }

        userRepository.findByEmail(email).ifPresent(user -> {
            throw new IllegalArgumentException("이미 존재하는 이메일 입니다.");
        });
        String authNum;
        //이메일 전송만을 담당하는 객체를 이용해서 이메일 로직 작성.
        try {
             authNum = mailSenderService.joinMail(email);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 전송 과정 중 문제 발생!");
        }

        //인증 코드 redis 에 저장
        String key = VERIFYCATION_CODE_KEY + email;
        redisTemplate.opsForValue().set(key, authNum, Duration.ofMinutes(1));
        return authNum;
    }


    //인증코드 검증 로직
    public Map<String, String> verifyEmail(Map<String, String> map) {

        // 차단 상태 확인
        if(isBlocked(map.get("email"))){
            throw new IllegalArgumentException("잘못된 횟수가 과다하여 임시 차단 중입니다. 잠시 후에 시도해주세요.");
        }


        // 인증 코드 조회
        String key = VERIFYCATION_CODE_KEY + map.get("email");
        Object foundCode = redisTemplate.opsForValue().get(key);
        if (foundCode == null) {
            throw new IllegalArgumentException("인증 코드가 만료 되었습니다.");
        }

        // 인증 시도 횟수 증가
        int attemptCount = incrementAttemptCount(map.get("email"));

        // 조회한 코드와 사용자가 입력한 인증번호 검증
        if(!foundCode.toString().equals(map.get("code"))) {
            //최대 시도 횟수 초과시 차단
            if(attemptCount >= 3){
                blockUser(map.get("email"));
                throw new IllegalArgumentException("email blocked");
            }
            int remainingAttempts = 3 - attemptCount;

            throw new IllegalArgumentException(String.format("인증 코드가 올바르지 않습니다!, %d", remainingAttempts));
        }

        log.info("이메일 인증 성공!, email={}", map.get("email"));
        redisTemplate.delete(key); // 레디스에서 인증번호 삭제
        return map;
    }

    private boolean isBlocked(String email){
        String key = VERIFYCATION_BLOCK_KEY + email;
        return redisTemplate.hasKey(key);
    }

    private void blockUser(String email) {
        String key = VERIFYCATION_BLOCK_KEY + email;
        redisTemplate.opsForValue().set(key, "blocked", Duration.ofMinutes(30));
    }

    private int incrementAttemptCount(String email){
        String key = VERIFYCATION_ATTEMPT_KEY + email;
        Object obj = redisTemplate.opsForValue().get(key);

        int count = (obj != null) ? Integer.parseInt(obj.toString()) +1 : 1;
        redisTemplate.opsForValue().set(key, count, Duration.ofMinutes(1));
        return count;
    }

    // 인가 코드로 카카오 엑세스 토큰 받기
    public String getKakaoAccessToken(String code) {
        RestTemplate restTemplate = new RestTemplate();

        String requestUri = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");

        System.out.println(redirectUri);
        System.out.println(kakaoClientId);

        //바디정보 세팅
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("code", code);
        params.add("redirect_uri", redirectUri);
        params.add("client_id", kakaoClientId );

        //헤더 정보와 바디정보를 하나로 합치자
        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        /*
            - RestTemplate객체가 REST API 통신을 위한 API인데 (자바스크립트 fetch역할)
            - 서버에 통신을 보내면서 응답을 받을 수 있는 메서드가 exchange
            param1: 요청 URL
            param2: 요청 방식 (get, post, put, patch, delete...)
            param3: 요청 헤더와 요청 바디 정보 - HttpEntity로 포장해서 줘야 함
            param4: 응답결과(JSON)를 어떤 타입으로 받아낼 것인지 (ex: DTO로 받을건지 Map으로 받을건지)
         */
        //요청 보내기 (토큰 발급)
        ResponseEntity<Map> responseEntity = restTemplate.exchange(
                requestUri, HttpMethod.POST, request, Map.class
        );

        // 응답 데이터에서 JSON 추출
        Map<String, Object> reponseJSON = (Map<String, Object>) responseEntity.getBody();

        log.info("응답 데이터:{}", reponseJSON);

        // Access Token 추출  (카카오 로그인 중인 사용자의 정보를 요청할 때 필요한 토큰)
        String accessToken = (String)reponseJSON.get("access_token");
        return accessToken;

    }


    //Access Token으로 사용자 정보 얻어오기!
    public KakaoUserDto getKakaoUserInfo(String kakaoAccessToken) {
        String requestUri = "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
        headers.add("Authorization", "Bearer " + kakaoAccessToken);

        //요청보내기
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<KakaoUserDto> response = restTemplate.exchange(
                requestUri, HttpMethod.POST, new HttpEntity<>(headers), KakaoUserDto.class
        );

        KakaoUserDto dto = response.getBody();
        log.info("응답된 사용자 정보:{}", dto);

        return dto;

    }

    public UserResDto findOrCreateKakaoUser(KakaoUserDto dto, String clientType) {
        //카카오 ID로 기존 사용자 찾기

        Optional<User> existingUser = userRepository.findBySocialProviderAndSocialId("KAKAO", dto.getId().toString());

        if (existingUser.isPresent()) {
            User foundUser = existingUser.get();
            return foundUser.fromEntity();
        }else{ // 처음 로그인 한사람이면 -> 새로 사용자 생성
            if(clientType.equals("admin")){
                User kakao = User.builder()
                        .email(dto.getAccount().getEmail())
                        .name(dto.getProperties().getNickname())
                        .profileImage(dto.getProperties().getProfileImage())
                        .role(Role.ADMIN)
                        .socialProvider("KAKAO")
                        .socialId(dto.getId().toString())
                        .password(null)// 외부 로그인이라 정보없음.
                        .address(null) // 필요하다면 따로 페이지 만들기
                        .registeredAt(LocalDateTime.now())
                        .status(UserStatus.ACTIVE)
                        .build();
                User saved = userRepository.save(kakao);
                return saved.fromEntity();
            }else {
                User kakao = User.builder()
                        .email(dto.getAccount().getEmail())
                        .name(dto.getProperties().getNickname())
                        .profileImage(dto.getProperties().getProfileImage())
                        .role(Role.USER)
                        .socialProvider("KAKAO")
                        .socialId(dto.getId().toString())
                        .password(null)// 외부 로그인이라 정보없음.
                        .address(null) // 필요하다면 따로 페이지 만들기
                        .registeredAt(LocalDateTime.now())
                        .status(UserStatus.ACTIVE)
                        .build();
                User saved = userRepository.save(kakao);
                return saved.fromEntity();
            }

        }
    }
}









