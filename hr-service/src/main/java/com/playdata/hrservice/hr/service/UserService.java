package com.playdata.hrservice.hr.service;

import com.playdata.hrservice.common.configs.AwsS3Config;
import com.playdata.hrservice.hr.dto.*;
import com.playdata.hrservice.hr.entity.*;
import com.playdata.hrservice.hr.repository.DepartmentRepository;
import com.playdata.hrservice.hr.repository.PositionRepository;
import com.playdata.hrservice.hr.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    //필요한 객체 생성하여 주입
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final PasswordEncoder encoder;
    private final AwsS3Config awsS3Config;
    private final RedisTemplate<String, Object>  redisTemplate;
    private final MailSenderService mailSenderService;

    // 회원가입
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

        Long departmentId = dto.getDepartmentId();
        Department foundDept = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with ID: " + departmentId));
        Long positionId = dto.getPositionId();
        Position foundPos = positionRepository.findById(positionId)
                .orElseThrow(() -> new EntityNotFoundException("Position not found with ID: " + positionId));

        // User Entity 생성 및 저장
        User newUser = User.builder()
                .userName(dto.getUserName())
                .email(dto.getEmail())
                .password(finalEncodedPassword)
                .address(dto.getAddress())
                .phone(dto.getPhone())
                .birthDate(dto.getBirthDate())
                .gender(dto.getGender())
                .department(foundDept)
                .position(foundPos)
                .hireDate(dto.getHireDate() != null ? dto.getHireDate() : LocalDate.now()) // 입사일이 없으면 오늘
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(newUser);
        return savedUser.fromEntity();
    }

    // 프로필
    public String uploadProfile(UserRequestDto dto) throws Exception{
        User user = userRepository.findById(dto.getEmployeeNo()).orElseThrow(
                () -> new EntityNotFoundException("User not found!")
        );

        // 1. 이전 프로필이 기본 url이 아니고, null도 아니라면 삭제
        String oldUrl = user.getProfileImage();
        if (oldUrl != null && !oldUrl.isBlank()) {
            awsS3Config.deleteFromS3Bucket(oldUrl);
        }

        // 2. 새 파일 업로드
        MultipartFile profileImage = dto.getProfileImage();
        String uniqueFileName = UUID.randomUUID() + "_" + profileImage.getOriginalFilename();
        String imageUrl = awsS3Config.uploadToS3Bucket(profileImage.getBytes(), uniqueFileName);

        user.setProfileImage(imageUrl);
        userRepository.save(user);
        return imageUrl;
    }

    // 로그인을 위한 Feign
    public UserLoginFeignResDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            return user.toUserLoginFeignResDto();
        }
        return null;
    }

    // 모든 서비스를 위한 Feign
    public UserFeignResDto getEmloyeeByEmail(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            return user.toUserFeignResDto();
        }
        return null;
    }

    // 비밀번호를 위한 Feign
    public void updatePassword(UserPwUpdateDto dto) {
        User user = userRepository.findByEmployeeNo(dto.getEmployeeNo()).orElseThrow(
                () -> new EntityNotFoundException("User not found!")
        );
        user.setPassword(dto.getNewPw());
        userRepository.save(user);
    }

//    // 사용자 정보 수정
//    @Transactional
//    public void updateUser(Long id, UserUpdateRequestDto dto, String hrRole) {
//        User user = userRepository.findByEmployeeNo(id).orElseThrow(
//                () -> new EntityNotFoundException("User not found!")
//        );
//        if (hrRole.equals("N")) {
//            throw new BadRequestException("수정 권한이 없습니다.");
//        }
//
//        user.setUserName(dto.getUserName());
//        user.setAddress(dto.getAddress());
//        user.setPhone(dto.getPhone());
//        user.setHireDate(dto.getHireDate());
//        user.setActivate(dto.getActivate());
//        user.setBirthDate(dto.getBirthDate());
//        user.setDepartment(departmentRepository.findByName(dto.getDepartmentName()));
//        user.setPosition(positionRepository.findByPositionName(dto.getPositionName()));
//        user.setProfileImage(awsS3Config.uploadToS3Bucket());
//        user.setRetireDate(dto.getRetireDate());
//
//        userRepository.save(user);
//    }

    // 직원 조회
    public Page<UserResDto> listUsers(Pageable pageable) {
        Page<User> users = userRepository.findAll(pageable);
        return users.map(user -> UserResDto.builder()
                .employeeNo(user.getEmployeeNo())
                .userName(user.getUserName())
                .positionName(user.getPosition().getPositionName())
                .departmentName(user.getDepartment().getName())
                .hireDate(user.getHireDate())
                .phone(user.getPhone())
                .email(user.getEmail())
                .activate(user.getActivate())
                .build());
    }


//    public User updateUser(Long userId, UserUpdateRequestDto dto) {
//        User user = userRepository.findById(userId).orElseThrow(
//                () -> new UsernameNotFoundException("회원 정보를 찾을 수 없습니다.")
//        );
//        if (!user.getEmail().equals(dto.getEmail())) {
//            if (userRepository.existsByEmail(dto.getEmail())) {
//                throw new IllegalArgumentException("이미 사용중인 이메일 입니다.");
//            }
//            user.setEmail(dto.getEmail());
//        }
//        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
//            String encodedPassword = encoder.encode(dto.getPassword());
//            user.setPassword(encodedPassword);
//        }
//
//        if (dto.getBirthDate() != null) {
//            user.setBirthDate(dto.getBirthDate());
//        }
//
//        user.setName(dto.getName());
//        user.setAddress(dto.getAddress());
//        user.setPhone(dto.getPhone());
//
//        return userRepository.save(user);
//    }
//
//    public User updateUserAddress(Long userId, String address) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        user.setAddress(address);
//        return userRepository.save(user);
//    }
//
//    @Transactional
//    public void deleteUser(Long userId) {
//        log.info("탈퇴 실행 시작 userId={}", userId);
//        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("회원 없음"));
//
//        user.setStatus(UserStatus.DELETED);
//        userRepository.save(user);
//
//        log.info("탈퇴 완료: userId={}, status={}", userId, user.getStatus());
//    }
//
//    public void restoreUser(Long userId) {
//        User user = userRepository.findById(userId).orElseThrow(() -> new UsernameNotFoundException("회원 없음"));
//
//        if (user.getStatus() == UserStatus.ACTIVE) {
//            throw new IllegalArgumentException("이미 활성화된 사용자입니다.");
//        }
//
//        user.setStatus(UserStatus.ACTIVE);
//        userRepository.save(user);
//    }
//
//    public List<UserResDto> userList(Pageable pageable) {
//
//        Page<User> users = userRepository.findAll(pageable);
//
//        List<User> content = users.getContent();
//        List<UserResDto> dtoList = content.stream()
//                .map(User::fromEntity)
//                .collect(Collectors.toList());
//
//        return dtoList;
//
//    }
//
//    public User findById(Long id) {
//        return userRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("User not found!"));
//    }
//
//    public UserResDto findByEmail(String email) {
//        User user = userRepository.findByEmail(email).orElseThrow(
//                () -> new EntityNotFoundException("User not found!")
//        );
//        return user.fromEntity();
//    }

//    public String mailCheck(String email) {
//
//        // 차단 상태 확인
//        if(isBlocked(email)){
//            throw new IllegalArgumentException("잘못된 요청 횟수가 과다하여 임시 차단 중입니다. 잠시 후에 시도해주세요.");
//        }
//
//        userRepository.findByEmail(email).ifPresent(user -> {
//            throw new IllegalArgumentException("이미 존재하는 이메일 입니다.");
//        });
//        String authNum;
//        //이메일 전송만을 담당하는 객체를 이용해서 이메일 로직 작성.
//        try {
//             authNum = mailSenderService.joinMail(email);
//        } catch (MessagingException e) {
//            throw new RuntimeException("이메일 전송 과정 중 문제 발생!");
//        }
//
//        //인증 코드 redis 에 저장
//        String key = VERIFYCATION_CODE_KEY + email;
//        redisTemplate.opsForValue().set(key, authNum, Duration.ofMinutes(1));
//        return authNum;
//    }


//    //인증코드 검증 로직
//    public Map<String, String> verifyEmail(Map<String, String> map) {
//
//        // 차단 상태 확인
//        if(isBlocked(map.get("email"))){
//            throw new IllegalArgumentException("잘못된 횟수가 과다하여 임시 차단 중입니다. 잠시 후에 시도해주세요.");
//        }
//
//
//        // 인증 코드 조회
//        String key = VERIFYCATION_CODE_KEY + map.get("email");
//        Object foundCode = redisTemplate.opsForValue().get(key);
//        if (foundCode == null) {
//            throw new IllegalArgumentException("인증 코드가 만료 되었습니다.");
//        }
//
//        // 인증 시도 횟수 증가
//        int attemptCount = incrementAttemptCount(map.get("email"));
//
//        // 조회한 코드와 사용자가 입력한 인증번호 검증
//        if(!foundCode.toString().equals(map.get("code"))) {
//            //최대 시도 횟수 초과시 차단
//            if(attemptCount >= 3){
//                blockUser(map.get("email"));
//                throw new IllegalArgumentException("email blocked");
//            }
//            int remainingAttempts = 3 - attemptCount;
//
//            throw new IllegalArgumentException(String.format("인증 코드가 올바르지 않습니다!, %d", remainingAttempts));
//        }
//
//        log.info("이메일 인증 성공!, email={}", map.get("email"));
//        redisTemplate.delete(key); // 레디스에서 인증번호 삭제
//        return map;
//    }

//    private boolean isBlocked(String email){
//        String key = VERIFYCATION_BLOCK_KEY + email;
//        return redisTemplate.hasKey(key);
//    }
//
//    private void blockUser(String email) {
//        String key = VERIFYCATION_BLOCK_KEY + email;
//        redisTemplate.opsForValue().set(key, "blocked", Duration.ofMinutes(30));
//    }
//
//    private int incrementAttemptCount(String email){
//        String key = VERIFYCATION_ATTEMPT_KEY + email;
//        Object obj = redisTemplate.opsForValue().get(key);
//
//        int count = (obj != null) ? Integer.parseInt(obj.toString()) +1 : 1;
//        redisTemplate.opsForValue().set(key, count, Duration.ofMinutes(1));
//        return count;
//    }


}









