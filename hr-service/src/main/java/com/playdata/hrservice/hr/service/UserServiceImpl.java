package com.playdata.hrservice.hr.service;

import com.playdata.hrservice.common.auth.TokenUserInfo;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final PasswordEncoder encoder;
    private final AwsS3Config awsS3Config;

    @Transactional
    @Override
    public UserResDto createUser(UserSaveReqDto dto, String hrRole) {
        if (hrRole.equals("N")) {
            throw new BadRequestException("계정 생성 권한이 없습니다.");
        }

        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 존재하는 이메일 입니다!");
        }

        // 1. DTO에 내장된 toEntity()를 사용하여 '기본 뼈대'가 완성된 User 엔티티를 생성합니다.
        User newUser = dto.toEntity(encoder);

        // 2. 서비스 계층의 책임: '특수 부품' (Department, Position)을 DB에서 조회합니다.
        Department foundDept = departmentRepository.findById(dto.getDepartmentId())
                .orElseThrow(() -> new EntityNotFoundException("Department not found with ID: " + dto.getDepartmentId()));
        Position foundPos = positionRepository.findById(dto.getPositionId())
                .orElseThrow(() -> new EntityNotFoundException("Position not found with ID: " + dto.getPositionId()));

        // 3. 서비스 계층의 책임: 조회한 부품을 뼈대에 최종 조립(연관관계 설정)합니다.
        newUser.setDepartment(foundDept);
        newUser.setPosition(foundPos);

        // 4. 서비스 계층의 책임: 비즈니스 규칙(직책에 따른 HR 권한)을 적용합니다.
        // [개선] '매직 넘버' 대신 직책 이름을 사용하는 것이 더 명확합니다.
        String positionName = foundPos.getPositionName();
        if ("대표".equals(positionName) || "인사팀장".equals(positionName)) {
            newUser.setHrRole("Y");
        }

        // 5. 완성된 엔티티를 저장합니다.
        User savedUser = userRepository.save(newUser);
        return savedUser.fromEntity();

    }

    @Transactional
    @Override
    public String uploadProfile(UserRequestDto dto) throws Exception{
        User user = userRepository.findById(dto.getEmployeeNo()).orElseThrow(
                () -> new EntityNotFoundException("User not found!")
        );

        String oldUrl = user.getProfileImage();
        if (oldUrl != null && !oldUrl.isBlank()) {
            awsS3Config.deleteFromS3Bucket(oldUrl);
        }

        MultipartFile profileImage = dto.getProfileImage();
        String uniqueFileName = UUID.randomUUID() + "_" + profileImage.getOriginalFilename();
        String imageUrl = awsS3Config.uploadToS3Bucket(profileImage.getBytes(), uniqueFileName);

        user.setProfileImage(imageUrl);
        // [개선] @Transactional 환경이므로, 더티 체킹에 의해 자동 업데이트됩니다. save() 호출은 생략 가능합니다.
        // userRepository.save(user);
        return imageUrl;
    }

    @Transactional
    @Override
    public void updateUser(Long employeeNo, UserUpdateRequestDto dto, String hrRole) throws Exception {
        if ("N".equals(hrRole)) {
            throw new AccessDeniedException("수정 권한이 없습니다.");
        }

        // 1. 수정할 User 엔티티를 조회합니다. (트랜잭션 시작)
        User user = userRepository.findByEmployeeNo(employeeNo).orElseThrow(
                () -> new EntityNotFoundException("User not found!")
        );

        // 2. 역할을 분리한 private 헬퍼 메소드들을 순차적으로 호출합니다.
        // 이제 이 메소드는 어떤 작업들이 일어나는지 요약해서 보여주는 '목차' 역할을 합니다.
        updatePersonalDetails(user, dto);
        updateUserHrDetails(user, dto);
        updateProfileImageIfNeeded(user, dto);

        // 3. @Transactional에 의해 '더티 체킹'이 발생하므로 명시적인 save() 호출이 필요 없습니다.
    }

    /**
    * 사용자의 개인 정보(이름, 연락처, 주소 등)를 업데이트합니다.
    * @param user 영속성 컨텍스트에 있는 User 엔티티
    * @param dto 업데이트 요청 데이터
    */

    private void updatePersonalDetails(User user, UserUpdateRequestDto dto) {
        user.setUserName(dto.getUserName());
        user.setExternalEmail(dto.getExternalEmail());
        // [수정] DTO로부터 Address 객체를 그대로 받아 설정합니다.
        // 이를 위해 UserUpdateRequestDto의 address 필드도 Address 타입으로 변경합니다.
        user.setAddress(dto.getAddress());
        user.setPhone(dto.getPhone());
        user.setResidentRegNo(dto.getResidentRegNo());
        user.setBirthDate(dto.getBirthDate());
        user.setBankName(dto.getBankName());
        user.setAccountNumber(dto.getAccountNumber());
        user.setAccountHolder(dto.getAccountHolder());
    }

    /**
     * 사용자의 인사 정보(부서, 직책, 입사일, 상태 등)를 업데이트합니다.
     *
     * @param user 영속성 컨텍스트에 있는 User 엔티티
     * @param dto  업데이트 요청 데이터
     */
    private void updateUserHrDetails(User user, UserUpdateRequestDto dto) {
        Department department = departmentRepository.findById(dto.getDepartmentId()).orElseThrow(
                () -> new EntityNotFoundException("Department not found with ID: " + dto.getDepartmentId())
        );
        // findByPositionName이 Position을 반환한다고 가정하고 null 체크, Optional을 반환한다면 orElseThrow 사용
        Position position = positionRepository.findByPositionName(dto.getPositionName());
        if (position == null) {
            throw new EntityNotFoundException("Position not found with name: " + dto.getPositionName());
        }

        user.setDepartment(department);
        user.setPosition(position);
        user.setHireDate(dto.getHireDate());
        user.setActivate(dto.getActivate());
        user.setRetireDate(dto.getRetireDate());

        if (dto.getHrRole() != null && !dto.getHrRole().equals(user.getHrRole())) {
            user.setHrRole(dto.getHrRole());
        }
    }

    /**
     * 프로필 이미지가 요청에 포함된 경우에만 프로필 이미지를 업데이트합니다.
     *
     * @param user 영속성 컨텍스트에 있는 User 엔티티
     * @param dto  업데이트 요청 데이터
     */

    private void updateProfileImageIfNeeded(User user, UserUpdateRequestDto dto) throws Exception{
        if (dto.getProfileImage() != null && !dto.getProfileImage().isEmpty()) {
            // 기존에 @Transactional이 추가된 uploadProfile 메소드를 호출합니다.
            // 스프링의 트랜잭션 전파(Propagation) 기본 설정(REQUIRED)에 따라,
            // 이 작업은 updateUser의 기존 트랜잭션에 참여하게 됩니다.
            uploadProfile(UserRequestDto.builder()
                    .employeeNo(user.getEmployeeNo())
                    .profileImage(dto.getProfileImage())
                    .build());
        }
    }


    @Override
    public UserResDto getUserByEmployeeNo(Long employeeNo) {
        return userRepository.findByEmployeeNo(employeeNo).orElseThrow(
                () -> new EntityNotFoundException("해당 직원은 존재하지 않습니다.")
        ).fromEntity();
    }

    @Override
    public UserLoginFeignResDto getUserByEmail(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            return user.toUserLoginFeignResDto();
        }
        return null;
    }

    @Transactional(readOnly = true)
    @Override
    public UserFeignResDto getEmployeeByEmployeeNo(Long employeeNo) {
        User user = userRepository.findByEmployeeNo(employeeNo)
                .orElseThrow(() -> new EntityNotFoundException("User not found with employeeNo: " + employeeNo));
        return user.toUserFeignResDto();
    }

    @Override
    public List<UserFeignResDto> getEmployeeByUserName(String userName) {
        List<User> users = userRepository.findByUserNameContaining(userName);
        return users.stream()
                .map(User::toUserFeignResDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserFeignResDto getEmployeeByEmail(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            return user.toUserFeignResDto();
        }
        return null;
    }

    @Override
    public UserFeignResDto getEmployeeById(Long employeeNo) {
        User user = userRepository.findByEmployeeNo(employeeNo).orElse(null);
        if (user != null) {
            log.info("Found user in DB: {}", user);
            UserFeignResDto dto = user.toUserFeignResDto();
            log.info("Converted UserFeignResDto: {}", dto);
            return dto;
        }
        return null;
    }

    @Override
    @Transactional
    public void updatePassword(String userEmail, UserPwUpdateDto dto) {
        User user = userRepository.findByEmail(userEmail).orElseThrow(
                () -> new EntityNotFoundException("User not found!")
        );

        // [보안 강화] 새로운 비밀번호를 반드시 암호화하여 저장합니다.
        String encodedNewPassword = encoder.encode(dto.getNewPw());

        // 객체의 상태만 변경(이 순간 'Dirty Checking' 발생)
        user.setPassword(encodedNewPassword);

        // 4. save()를 호출하지 않아도, 메소드가 끝나면 JPA가 자동으로 DB에 UPDATE 쿼리를 실행합니다.
        // userRepository.save(user); // 이 코드는 불필요하므로 생략 가능합니다.
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserListResDto> listUsers(Pageable pageable, String hrRole) {
        // [수정] 문법 오류를 수정하고, Address 객체를 문자열로 조합하는 로직을 완성합니다.
        if (!"Y".equals(hrRole)) {
            throw new AccessDeniedException("인사 관리 목록을 조회할 권한이 없습니다.");
        }

        Page<User> usersPage = userRepository.findAllWithDetails(pageable);

        return usersPage.map(user -> {
            String fullAddress = "";
            if (user.getAddress() != null) {
                fullAddress = (user.getAddress().getCity() != null ? user.getAddress().getCity() : "")
                        + " " + (user.getAddress().getStreet() != null ? user.getAddress().getStreet() : "");
                fullAddress = fullAddress.trim();
            }

            //[수정] .builder() 대신, UserListResDto에 정의된 생성자를 직접 호출합니다.
            //이 생성자는 우리가 QueryDSL을 위해 만들었던 @QueryProjection 생성자와 동일합니다.
            return new UserListResDto(
                    user.getEmployeeNo(),
                    user.getUserName(),
                    user.getPosition().getPositionName(),
                    user.getDepartment().getDepartmentId(),
                    user.getDepartment().getName(),
                    user.getHireDate(),
                    user.getPhone(),
                    user.getEmail(),
                    fullAddress,
                    user.getActivate()
            );
        });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserListResDto> searchUsers(UserSearchCondition condition, Pageable pageable) {
        // [개선] 인터페이스와 구현체의 메소드 이름을 'searchUsers'로 일치시킵니다.
        return userRepository.searchUsers(condition, pageable);
    }

    @Override
    @Transactional
    public void retireUser(Long employeeNo, String hrRole) {
        if (!"Y".equals(hrRole)) {
            throw new AccessDeniedException("HR 권한이 필요합니다.");
        }
        User user = userRepository.findByEmployeeNo(employeeNo).orElseThrow(
                () -> new EntityNotFoundException("해당 사번 없음.")
        );
        user.setRetireDate(LocalDate.now());
        user.setActivate("N");
        // 더티 체킹에 의해 자동으로 업데이트됩니다.
    }

    @Override
    public boolean hasApprovedExternalSchedule(Long userId, LocalDate date) {
        return false;
    }

    @Override
    public String getApprovedExternalScheduleType(Long userId, LocalDate date) {
        return null;
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserResDto> getUsersByIds(List<Long> employeeNos) {
        List<User> users = userRepository.findByEmployeeNoIn(employeeNos);
        return users.stream()
                .map(User::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<UserResDto> getUsersWithFirstAnniversaryInMonth(int year, int month) {
        List<User> users = userRepository.findUsersWithHireDateInMonth(year, month);
        return users.stream()
                .map(User::fromEntity)
                .collect(Collectors.toList());
    }

}









