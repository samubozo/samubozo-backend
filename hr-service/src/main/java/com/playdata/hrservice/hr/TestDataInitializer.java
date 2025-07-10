package com.playdata.hrservice.hr;

import com.playdata.hrservice.hr.entity.Department;
import com.playdata.hrservice.hr.entity.Position;
import com.playdata.hrservice.hr.entity.User;
import com.playdata.hrservice.hr.repository.DepartmentRepository;
import com.playdata.hrservice.hr.repository.PositionRepository;
import com.playdata.hrservice.hr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_PROFILE_IMAGE_URL = "https://i.pravatar.cc/150?img=";

    @Override
    public void run(String... args) throws Exception {
        // 부서 더미 데이터 생성 및 저장
        createDepartment(1L, "경영지원", "#FFAB91");
        createDepartment(2L, "인사팀", "#B39DDB");
        createDepartment(3L, "회계팀", "#81D4FA");
        createDepartment(4L, "영업팀", "#A5D6A7");

        // 직책 더미 데이터 생성 및 저장
        createPosition(1L, "사장", "Y");
        createPosition(2L, "부장", "Y");
        createPosition(3L, "책임", "N");
        createPosition(4L, "선임", "N");
        createPosition(5L, "사원", "N");

        // test1 계정 생성: 사장, 경영지원
        createTestUser("test1", "test1@s.com", "1234", 1L, 1L, DEFAULT_PROFILE_IMAGE_URL + "1");

        // test2 계정 생성: 부장, 인사팀
        createTestUser("test2", "test2@s.com", "1234", 2L, 2L, DEFAULT_PROFILE_IMAGE_URL + "2");

        // test3 계정 생성: 책임, 회계팀
        createTestUser("test3", "test3@s.com", "1234", 3L, 3L, DEFAULT_PROFILE_IMAGE_URL + "3");

        // test4 계정 생성: 사원, 영업팀
        createTestUser("test4", "test4@s.com", "1234", 5L, 4L, DEFAULT_PROFILE_IMAGE_URL + "4");

        // test5 계정 생성: 선임, 경영지원
        createTestUser("test5", "test5@s.com", "1234", 4L, 1L, DEFAULT_PROFILE_IMAGE_URL + "5");
    }

    private void createDepartment(Long id, String name, String color) {
        if (departmentRepository.findById(id).isEmpty()) {
            Department department = Department.builder()
                    .departmentId(id)
                    .name(name)
                    .departmentColor(color)
                    .build();
            departmentRepository.save(department);
            log.info("Department {} created.", name);
        } else {
            log.info("Department {} already exists. Skipping creation.", name);
        }
    }

    private void createPosition(Long id, String name, String hrRole) {
        if (positionRepository.findById(id).isEmpty()) {
            Position position = Position.builder()
                    .positionId(id)
                    .positionName(name)
                    .hrRole(hrRole)
                    .build();
            positionRepository.save(position);
            log.info("Position {} created.", name);
        } else {
            log.info("Position {} already exists. Skipping creation.", name);
        }
    }

    private void createTestUser(String userName, String email, String password, Long positionId, Long departmentId, String profileImageUrl) {
        if (userRepository.findByEmail(email).isEmpty()) {
            log.info("Creating test user: {}", email);

            Department department = Department.builder().departmentId(departmentId).build();
            Position position = Position.builder().positionId(positionId).build();

            User testUser = User.builder()
                    .userName(userName)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .phone("010-1234-5678")
                    .birthDate(LocalDate.of(1990, 1, 1))
                    .gender("M")
                    .createdAt(LocalDateTime.now())
                    .hireDate(LocalDate.of(2023, 1, 1))
                    .address("서울시 강남구")
                    .profileImage(profileImageUrl)
                    .retireDate(null)
                    .activate("Y")
                    .department(department)
                    .position(position)
                    .build();

            userRepository.save(testUser);
            log.info("Test user {} created successfully.", email);
        } else {
            log.info("Test user {} already exists. Skipping creation.", email);
        }
    }
}
