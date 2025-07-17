package com.playdata.hrservice.hr.service;

import com.playdata.hrservice.common.configs.AwsS3Config;
import com.playdata.hrservice.hr.dto.*;
import com.playdata.hrservice.hr.entity.Department;
import com.playdata.hrservice.hr.entity.Position;
import com.playdata.hrservice.hr.entity.User;
import com.playdata.hrservice.hr.repository.DepartmentRepository;
import com.playdata.hrservice.hr.repository.PositionRepository;
import com.playdata.hrservice.hr.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    //필요한 객체 생성하여 주입
    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final PasswordEncoder encoder;
    private final AwsS3Config awsS3Config;

    // 회원가입
    @Transactional
    @Override
    public UserResDto createUser(UserSaveReqDto dto, String hrRole) {
        if (hrRole.equals("N")) {
            throw new BadRequestException("계정 생성 권한이 없습니다.");
        }

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
    @Override
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
    @Override
    public void updateUser(Long employeeNo, UserUpdateRequestDto dto, String hrRole) throws Exception {
        User user = userRepository.findByEmployeeNo(employeeNo).orElseThrow(
                () -> new EntityNotFoundException("User not found!")
        );
        if (hrRole.equals("N")) {
            throw new BadRequestException("수정 권한이 없습니다.");
        }

        user.setUserName(dto.getUserName());
        user.setExternalEmail(dto.getExternalEmail());
        user.setAddress(dto.getAddress());
        user.setPhone(dto.getPhone());
        user.setResidentRegNo(dto.getResidentRegNo());
        user.setHireDate(dto.getHireDate());
        user.setActivate(dto.getActivate());
        user.setBirthDate(dto.getBirthDate());
        user.setRemarks(dto.getRemarks());
        user.setBankName(dto.getBankName());
        user.setAccountNumber(dto.getAccountNumber());
        user.setAccountHolder(dto.getAccountHolder());
//        user.setDepartment(departmentRepository.findByName(dto.getDepartmentName()));
        user.setDepartment(departmentRepository.findById(dto.getDepartmentId()).orElseThrow(
                () -> new EntityNotFoundException("Department not found with ID: " + dto.getDepartmentId())
        ));
        user.setPosition(positionRepository.findByPositionName(dto.getPositionName()));
        if (dto.getProfileImage() != null && !dto.getProfileImage().isEmpty()) {
            uploadProfile(UserRequestDto.builder()
                    .employeeNo(user.getEmployeeNo())
                    .profileImage(dto.getProfileImage())
                    .build());
        }
        user.setRetireDate(dto.getRetireDate());

        userRepository.save(user);
    }

    // 직원 상세 조회
    @Override
    public UserResDto getUserByEmployeeNo(Long employeeNo) {
        return userRepository.findByEmployeeNo(employeeNo).orElseThrow(
                () -> new EntityNotFoundException("해당 직원은 존재하지 않습니다.")
        ).fromEntity();
    }

    // 로그인을 위한 Feign
    @Override
    public UserLoginFeignResDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            return user.toUserLoginFeignResDto();
        }
        return null;
    }

    // Feign client용: employeeNo로 사용자 정보 조회
    @Transactional(readOnly = true)
    @Override
    public UserFeignResDto getEmployeeByEmployeeNo(Long employeeNo) {
        User user = userRepository.findByEmployeeNo(employeeNo)
                .orElseThrow(() -> new EntityNotFoundException("User not found with employeeNo: " + employeeNo));
        return user.toUserFeignResDto();
    }

    // Feign client용: userName으로 사용자 정보 조회
    @Override
    public List<UserFeignResDto> getEmployeeByUserName(String userName) {
        List<User> users = userRepository.findByUserNameContaining(userName);
        return users.stream()
                .map(User::toUserFeignResDto)
                .collect(Collectors.toList());
    }

    // 모든 서비스를 위한 Feign
    @Override
    public UserFeignResDto getEmployeeByEmail(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            return user.toUserFeignResDto();
        }
        return null;
    }

    // 모든 서비스를 위한 Feign (id로 조회)
    @Override
    public UserFeignResDto getEmployeeById(Long employeeNo) {
        User user = userRepository.findByEmployeeNo(employeeNo).orElse(null);
        if (user != null) {
            return user.toUserFeignResDto();
        }
        return null;
    }

    // 비밀번호를 위한 Feign
    @Override
    public void updatePassword(UserPwUpdateDto dto) {
        User user = userRepository.findByEmployeeNo(dto.getEmployeeNo()).orElseThrow(
                () -> new EntityNotFoundException("User not found!")
        );
        user.setPassword(dto.getNewPw());
        userRepository.save(user);
    }

    // 직원 조회
    @Override
    public Page<UserResDto> listUsers(Pageable pageable, String hrRole) {
        Page<User> users = userRepository.findAll(pageable);

        return users.map(user -> UserResDto.builder()
                .employeeNo(user.getEmployeeNo())
                .userName(user.getUserName())
                .positionName(user.getPosition().getPositionName())
                .department(new DepartmentResDto(user.getDepartment())) // DepartmentResDto 객체로 변경
                .hireDate(user.getHireDate())
                .phone(user.getPhone())
                .email(user.getEmail())
                .address(user.getAddress())
                .activate(user.getActivate())
                .profileImage(user.getProfileImage())
                .hrRole(user.getPosition().getHrRole())
                .build());
    }

    // 사용자 검색 (조건에 따라 페이징 또는 전체 리스트 반환)
    @Override
    public Object searchUsers(String userName, String departmentName, String hrRole, Pageable pageable) {
        if (StringUtils.hasText(userName) || StringUtils.hasText(departmentName) || StringUtils.hasText(hrRole)) {
            // 검색 조건이 있을 경우, 페이징 없이 전체 리스트 반환
            List<User> users;
            if (StringUtils.hasText(userName) && StringUtils.hasText(departmentName) && StringUtils.hasText(hrRole)) {
                users = userRepository.findByUserNameContainingAndDepartmentNameContainingAndPositionHrRole(userName, departmentName, hrRole);
            } else if (StringUtils.hasText(userName) && StringUtils.hasText(departmentName)) {
                users = userRepository.findByUserNameContainingAndDepartmentNameContaining(userName, departmentName);
            } else if (StringUtils.hasText(userName) && StringUtils.hasText(hrRole)) {
                users = userRepository.findByUserNameContainingAndPositionHrRole(userName, hrRole);
            } else if (StringUtils.hasText(departmentName) && StringUtils.hasText(hrRole)) {
                users = userRepository.findByDepartmentNameContainingAndPositionHrRole(departmentName, hrRole);
            } else if (StringUtils.hasText(userName)) {
                users = userRepository.findByUserNameContaining(userName);
            } else if (StringUtils.hasText(departmentName)) {
                users = userRepository.findByDepartmentNameContaining(departmentName);
            } else { // hrRole만 있을 경우
                users = userRepository.findByPositionHrRole(hrRole);
            }
            return users.stream().map(user -> UserResDto.builder()
                    .employeeNo(user.getEmployeeNo())
                    .userName(user.getUserName())
                    .positionName(user.getPosition().getPositionName())
                    .department(new DepartmentResDto(user.getDepartment())) // DepartmentResDto 객체로 변경
                    .hireDate(user.getHireDate())
                    .phone(user.getPhone())
                    .email(user.getEmail())
                    .address(user.getAddress())
                    .activate(user.getActivate())
                    .profileImage(user.getProfileImage())
                    .hrRole(user.getPosition().getHrRole())
                    .build()).collect(Collectors.toList());
        } else {
            // 검색 조건이 없을 경우, 페이징 적용
            Page<User> users = userRepository.findAll(pageable);
            return users.map(user -> UserResDto.builder()
                    .employeeNo(user.getEmployeeNo())
                    .userName(user.getUserName())
                    .positionName(user.getPosition().getPositionName())
                    .department(new DepartmentResDto(user.getDepartment())) // DepartmentResDto 객체로 변경
                    .hireDate(user.getHireDate())
                    .phone(user.getPhone())
                    .email(user.getEmail())
                    .address(user.getAddress())
                    .activate(user.getActivate())
                    .profileImage(user.getProfileImage())
                    .hrRole(user.getPosition().getHrRole())
                    .build());
        }
    }

    // 퇴사 처리
    @Override
    public void retireUser(Long employeeNo, String hrRole) {
        if (!"Y".equals(hrRole)) {
            throw new AccessDeniedException("HR 권한이 필요합니다.");
        }
        User user = userRepository.findByEmployeeNo(employeeNo).orElseThrow(
                () -> new EntityNotFoundException("해당 사번 없음.")
        );
        user.setRetireDate(LocalDate.now());
        user.setActivate("N");
        userRepository.save(user);
    }

    // 특정 사용자가 특정 날짜에 승인된 외부 일정(출장, 연수 등)이 있는지 확인
    @Override
    public boolean hasApprovedExternalSchedule(Long userId, LocalDate date) {
        // 현재는 항상 false 반환 (임시)
        return false;
    }

    // 특정 사용자가 특정 날짜에 승인된 외부 일정의 종류를 조회
    @Override
    public String getApprovedExternalScheduleType(Long userId, LocalDate date) {
        // 현재는 항상 null 반환 (임시)
        return null;
    }

    /**
     * 주어진 ID 목록에 해당하는 사용자 정보를 조회합니다.
     *
     * @param employeeNos 조회할 사용자 ID 목록
     * @return 사용자 정보 DTO 목록
     */
    @Transactional(readOnly = true)
    @Override
    public List<UserResDto> getUsersByIds(List<Long> employeeNos) {
        List<User> users = userRepository.findByEmployeeNoIn(employeeNos);
        return users.stream()
                .map(User::fromEntity)
                .collect(Collectors.toList());
    }
}









