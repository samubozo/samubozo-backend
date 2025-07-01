package com.playdata.hrservice.hr.service;

import com.playdata.hrservice.common.auth.TokenUserInfo;
import com.playdata.hrservice.hr.dto.*;
import com.playdata.hrservice.hr.entity.*;
import com.playdata.hrservice.hr.repository.DepartmentRepository;
import com.playdata.hrservice.hr.repository.PositionRepository;
import com.playdata.hrservice.hr.repository.RoleRepository;
import com.playdata.hrservice.hr.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.persistence.EntityNotFoundException;
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
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDate;
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
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final RoleRepository roleRepository;
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


    @Transactional
    public UserResDto createUser(UserSaveReqDto dto) {
        // 이메일 중복 확인 (신규 가입)
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이메일 입니다!");
        }

        // 일반 회원가입 사용자: 비밀번호가 필수로 존재해야 함
        if (!StringUtils.hasText(dto.getPassword())) { // 비밀번호가 null이거나 빈 문자열인지 확인
            throw new IllegalArgumentException("비밀번호는 필수입니다.");
        }
        // 비밀번호 길이 검사 (패턴 검사도 필요하다면 여기에 추가)
        if (dto.getPassword().length() < 8) {
            throw new IllegalArgumentException("비밀번호는 최소 8자 이상이어야 합니다.");
        }
        String finalEncodedPassword = encoder.encode(dto.getPassword());

        // 3. 연관관계 엔티티 조회 (FK)
        Department department = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new IllegalArgumentException("부서 정보가 올바르지 않습니다."));
        Position position = positionRepository.findById(dto.getPositionId())
                .orElseThrow(() -> new IllegalArgumentException("직책 정보가 올바르지 않습니다."));
        Role role = roleRepository.findById(dto.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("역할 정보가 올바르지 않습니다."));

        // User Entity 생성 및 저장
        User newUser = User.builder()
                .userName(dto.getUserName())
                .email(dto.getEmail())
                .password(finalEncodedPassword)
                .address(dto.getAddress())
                .phone(dto.getPhone())
                .birthDate(dto.getBirthDate())
                .gender(dto.getGender())
                .activate(dto.getActivate() != null ? dto.getActivate() : "Y")
                .department(department)
                .position(position)
                .role(role)
                .hireDate(dto.getHireDate() != null ? dto.getHireDate() : LocalDate.now()) // 입사일이 없으면 오늘
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(newUser);
        return savedUser.fromEntity();
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


}









