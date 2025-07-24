package com.playdata.authservice.auth.service;

import com.playdata.authservice.auth.dto.UserLoginFeignResDto;
import com.playdata.authservice.auth.dto.UserLoginReqDto;
import com.playdata.authservice.auth.dto.UserPwUpdateDto;
import com.playdata.authservice.client.HrServiceClient;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final PasswordEncoder encoder;
    private final RedisTemplate<String, Object>  redisTemplate;
    private final MailSenderService mailSenderService;

    private final HrServiceClient hrServiceClient;

    private static final String CODE_CHARS =
            "0123456789" + "ABCDEFGHIJKLMNOPQRSTUVWXYZ" + "abcdefghijklmnopqrstuvwxyz";
    private static final SecureRandom random = new SecureRandom();

    private static final String VERIFICATION_CODE_KEY = "email_verify:code:";
    private static final String VERIFICATION_ATTEMPT_KEY = "email_verify:attempt:";
    private static final String VERIFICATION_BLOCK_KEY = "email_verify:block:";
    private static final String RESET_KEY_PREFIX = "pw-reset:";
    private static final Duration RESET_CODE_TTL = Duration.ofMinutes(5);

    @Override
    public UserLoginFeignResDto login(UserLoginReqDto dto) {

        UserLoginFeignResDto userLoginFeignResDto = hrServiceClient.getLoginUser(dto.getEmail());

        log.info("userLoginFeignResDto: {}", userLoginFeignResDto);
        if (!"Y".equals(userLoginFeignResDto.getActivate())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "정지된 계정입니다.");
        }

        if (!encoder.matches(dto.getPassword(), userLoginFeignResDto.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }

        return userLoginFeignResDto;
    }

    @Override
    public void sendPasswordResetCode(@NotBlank(message = "이메일을 입력해 주세요.") String email) {
        UserLoginFeignResDto userLoginFeignResDto = hrServiceClient.getLoginUser(email);
        if (userLoginFeignResDto == null) {
            throw new EntityNotFoundException("해당 이메일의 사용자를 찾을 수 없습니다.");
        }

        String code = makeAlphanumericCode(9);

        String redisKey = RESET_KEY_PREFIX + email;
        redisTemplate.opsForValue()
                .set(redisKey, code, RESET_CODE_TTL);

        try {
            mailSenderService.sendPasswordResetMail(email, userLoginFeignResDto.getUserName(), code);
        } catch (MessagingException e) {
            log.error("비밀번호 재설정 메일 전송 실패", e);
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "인증 메일 전송에 실패했습니다. 잠시 후 다시 시도해 주세요."
            );
        }
    }

    @Override
    public String makeAlphanumericCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = random.nextInt(CODE_CHARS.length());
            sb.append(CODE_CHARS.charAt(idx));
        }
        String code = sb.toString();
        log.info("생성된 비밀번호 재설정 코드:{}", code);
        return code;
    }

    @Override
    public void verifyResetCode(@NotBlank String email, @NotBlank String code) {
        UserLoginFeignResDto userLoginFeignResDto = hrServiceClient.getLoginUser(email);
        if (userLoginFeignResDto == null) {
            throw new EntityNotFoundException("해당 이메일의 사용자를 찾을 수 없습니다.");
        }

        int attemptCount = incrementAttemptCount(email);

        if(attemptCount >= 5){
            blockUser(email);
            throw new IllegalArgumentException("너무 많이 시도하셨습니다. 30분 후에 다시 시도해주세요.");
        }

        String key = RESET_KEY_PREFIX + email;
        Object savedCode = redisTemplate.opsForValue().get(key);
        if (savedCode == null) {
            throw new IllegalArgumentException("인증 코드가 만료되었습니다. 다시 요청해 주세요.");
        }
        if (!savedCode.equals(code)) {
            throw new IllegalArgumentException("인증 코드가 일치하지 않습니다. 5회 틀릴시 요청이 30분간 제한됩니다.");
        }
    }

    @Override
    public void resetPassword(@NotBlank String email, @NotBlank String code, @NotBlank String newPassword) {
        verifyResetCode(email, code);

        UserLoginFeignResDto userLoginFeignResDto = hrServiceClient.getLoginUser(email);
        if (userLoginFeignResDto == null) {
            throw new EntityNotFoundException("해당 이메일의 사용자를 찾을 수 없습니다.");
        }

        String encoded = encoder.encode(newPassword);
        UserPwUpdateDto userPwUpdateDto = UserPwUpdateDto.builder()
                .employeeNo(userLoginFeignResDto.getEmployeeNo())
                .newPw(encoded)
                .build();
        hrServiceClient.setPassword(userPwUpdateDto);

        redisTemplate.delete(RESET_KEY_PREFIX + email);
    }

    @Override
    public String mailCheck(String email) {

        if(isBlocked(email)){
            throw new IllegalArgumentException("잘못된 요청 횟수가 과다하여 임시 차단 중입니다. 잠시 후에 시도해주세요.");
        }

        UserLoginFeignResDto userLoginFeignResDto = hrServiceClient.getLoginUser(email);
        if (userLoginFeignResDto != null) {
            throw new IllegalArgumentException("이미 존재하는 이메일 입니다.");
        }

        String authNum;
        try {
            authNum = mailSenderService.joinMail(email);
        } catch (MessagingException e) {
            throw new RuntimeException("이메일 전송 과정 중 문제 발생!");
        }

        String key = VERIFICATION_CODE_KEY + email;
        redisTemplate.opsForValue().set(key, authNum, Duration.ofMinutes(1));
        return authNum;
    }


    @Override
    public boolean isBlocked(String email){
        String key = VERIFICATION_BLOCK_KEY + email;
        return redisTemplate.hasKey(key);
    }

    @Override
    public void blockUser(String email) {
        String key = VERIFICATION_BLOCK_KEY + email;
        redisTemplate.opsForValue().set(key, "blocked", Duration.ofMinutes(30));
    }

    @Override
    public int incrementAttemptCount(String email){
        String key = VERIFICATION_ATTEMPT_KEY + email;
        Object obj = redisTemplate.opsForValue().get(key);

        int count = (obj != null) ? Integer.parseInt(obj.toString()) +1 : 1;
        redisTemplate.opsForValue().set(key, count, Duration.ofMinutes(1));
        return count;
    }

}








