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

    // 사용자 정보 수정
    @Transactional
    public void updateUser(Long id, UserUpdateRequestDto dto, String hrRole) throws Exception {
        User user = userRepository.findByEmployeeNo(id).orElseThrow(
                () -> new EntityNotFoundException("User not found!")
        );
        if (hrRole.equals("N")) {
            throw new BadRequestException("수정 권한이 없습니다.");
        }

        user.setUserName(dto.getUserName());
        user.setAddress(dto.getAddress());
        user.setPhone(dto.getPhone());
        user.setHireDate(dto.getHireDate());
        user.setActivate(dto.getActivate());
        user.setBirthDate(dto.getBirthDate());
        user.setDepartment(departmentRepository.findByName(dto.getDepartmentName()));
        user.setPosition(positionRepository.findByPositionName(dto.getPositionName()));
        if (user.getProfileImage() != null) {
            uploadProfile(UserRequestDto.builder()
                    .employeeNo(user.getEmployeeNo())
                    .profileImage(dto.getProfileImage())
                    .build());
        }
        user.setRetireDate(dto.getRetireDate());

        userRepository.save(user);
    }

    // 직원 리스트 조회
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

    // 직원 상세 조회
    public UserResDto getUserByEmployeeNo(Long employeeNo) {
        return userRepository.findByEmployeeNo(employeeNo).orElseThrow(
                () -> new EntityNotFoundException("해당 직원은 존재하지 않습니다.")
        ).fromEntity();
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

    // 증명서 신청


}









