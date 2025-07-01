package com.playdata.authservice.auth.service;

import com.playdata.authservice.auth.dto.*;

import com.playdata.authservice.auth.entity.User;
import com.playdata.authservice.auth.repository.RoleRepository;
import com.playdata.authservice.auth.repository.UserRepository;
import com.playdata.authservice.common.auth.Role;
import com.playdata.authservice.common.auth.TokenUserInfo;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    //필요한 객체 생성하여 주입
    private final UserRepository userRepository;
    private final PasswordEncoder encoder;
    private final RedisTemplate<String, Object>  redisTemplate;
    private final MailSenderService mailSenderService;

    private static final String CODE_CHARS =
            "0123456789" + "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom random = new SecureRandom();

    // Redis Key 상수
    private static final String VERIFICATION_CODE_KEY = "email_verify:code:";
    private static final String VERIFICATION_ATTEMPT_KEY = "email_verify:attempt:";
    private static final String VERIFICATION_BLOCK_KEY = "email_verify:block:";
    private static final String RESET_KEY_PREFIX = "pw-reset:";
    private static final Duration RESET_CODE_TTL = Duration.ofMinutes(5);

    // 로그인 인증
    public User login(UserLoginReqDto dto) {
        User user = userRepository.findByEmail(dto.getEmail()).orElseThrow(
                () -> new EntityNotFoundException("User not found!")
        );

        if (!"Y".equals(user.getActivate())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "정지된 계정입니다.");
        }

        if (!encoder.matches(dto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return user;
    }

    // 비밀번호 찾기(인증코드 발송)
    public void sendPasswordResetCode(@NotBlank(message = "이메일을 입력해 주세요.") String email) {
        // 1. 회원 존재 확인
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("해당 이메일의 사용자를 찾을 수 없습니다."));

        // 2. 인증 코드 생성
        String code = makeAlphanumericCode(9);

        // 3. Rediss에 저장
        String redisKey = RESET_KEY_PREFIX + email;
        redisTemplate.opsForValue()
                .set(redisKey, code, RESET_CODE_TTL);

        // 4. 비밀번호 재설정 메일 발송
        try {
            mailSenderService.sendPasswordResetMail(email, user.getUserName(), code);
        } catch (MessagingException e) {
            log.error("비밀번호 재설정 메일 전송 실패", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "인증 메일 전송에 실패했습니다. 잠시 후 다시 시도해 주세요."
            );
        }
    }

    private String makeAlphanumericCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = random.nextInt(CODE_CHARS.length());
            sb.append(CODE_CHARS.charAt(idx));
        }
        String code = sb.toString();
        log.info("생성된 비밀번호 재설정 코드:{}", code);
        return code;
    }

    // 비밀번호 재설정 인증코드 검증
    public void verifyResetCode(@NotBlank String email, @NotBlank String code) {
        // 1. 사용자 재확인 (옵션)
        userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다."));

        // 2. Redis에서 코드 조회
        String key = RESET_KEY_PREFIX + email;
        Object savedCode = redisTemplate.opsForValue().get(key);
        if (savedCode == null) {
            throw new IllegalArgumentException("인증 코드가 만료되었습니다. 다시 요청해 주세요.");
        }
        if (!savedCode.equals(code)) {
            throw new IllegalArgumentException("인증 코드가 일치하지 않습니다.");
        }
    }

    // 비밀번호 재설정(최종 적용)
    public void resetPassword(@NotBlank String email, @NotBlank String code, @NotBlank String newPassword) {
        verifyResetCode(email, code);

        // 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다."));

        // 비밀번호 해시 후 저장
        String encoded = encoder.encode(newPassword);
        user.setPassword(encoded);
        userRepository.save(user);

        // 사용한 코드 삭제
        redisTemplate.delete(RESET_KEY_PREFIX + email);
    }

    // 이메일 인증코드 발송
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
        String key = VERIFICATION_CODE_KEY + email;
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
        String key = VERIFICATION_CODE_KEY + map.get("email");
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

    // 인증 차단 여부 (이메일 인증용)
    private boolean isBlocked(String email){
        String key = VERIFICATION_BLOCK_KEY + email;
        return redisTemplate.hasKey(key);
    }

    // 인증 차단 (이메일 인증용)
    private void blockUser(String email) {
        String key = VERIFICATION_BLOCK_KEY + email;
        redisTemplate.opsForValue().set(key, "blocked", Duration.ofMinutes(30));
    }

    // 인증 시도 횟수 관리 (이메일 인증용)
    private int incrementAttemptCount(String email){
        String key = VERIFICATION_ATTEMPT_KEY + email;
        Object obj = redisTemplate.opsForValue().get(key);

        int count = (obj != null) ? Integer.parseInt(obj.toString()) +1 : 1;
        redisTemplate.opsForValue().set(key, count, Duration.ofMinutes(1));
        return count;
    }

}









