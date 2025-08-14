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
        createDepartment(1L, "경영지원", "#FFAB91", "https://images.unsplash.com/photo-1506744038136-46273834b3fb?auto=format&fit=facearea&w=400&h=400\n");
        createDepartment(2L, "인사팀", "#B39DDB", "https://images.unsplash.com/photo-1519125323398-675f0ddb6308?auto=format&fit=facearea&w=400&h=400\n");
        createDepartment(3L, "회계팀", "#81D4FA", "https://images.unsplash.com/photo-1515378791036-0648a3ef77b2?auto=format&fit=facearea&w=400&h=400\n");
        createDepartment(4L, "영업팀", "#A5D6A7", "https://images.unsplash.com/photo-1521737852567-6949f3f9f2b5?auto=format&fit=facearea&w=400&h=400");

        // 직책 더미 데이터 생성 및 저장
        createPosition(1L, "사장");
        createPosition(2L, "부장");
        createPosition(3L, "책임");
        createPosition(4L, "선임");
        createPosition(5L, "사원");

        // test1 계정 생성: 사장, 경영지원
        createTestUser("신현국", "uiuo1266@gmail.com", "1234", 1L, 1L, DEFAULT_PROFILE_IMAGE_URL + "12",
                "010-1111-2222",
                LocalDate.of(1996, 3, 11),
                "M",
                LocalDate.of(2010, 5, 1),
                "서울시 성동구",
                "960311-1084736");

        // test2 계정 생성: 부장, 인사팀
        createTestUser("이호영", "skyroad0704@gmail.com", "1234", 2L, 2L, DEFAULT_PROFILE_IMAGE_URL + "54",
                "010-9876-5432",
                LocalDate.of(1994, 12, 24),
                "M",
                LocalDate.of(2013, 4, 10),
                "서울시 동작구",
                "941224-1036482");

        // test3 계정 생성: 책임, 회계팀
        createTestUser("김예은", "yeeun6328@gmail.com", "1234", 3L, 3L, DEFAULT_PROFILE_IMAGE_URL + "23",
                "010-5555-8888",
                LocalDate.of(1997, 10, 26),
                "F",
                LocalDate.of(2018, 9, 18),
                "서울시 강서구",
                "971026-2022746");

        // test4 계정 생성: 사원, 영업팀
        createTestUser("강원빈", "fnfn1997@naver.com", "1234", 5L, 4L, DEFAULT_PROFILE_IMAGE_URL + "59",
                "010-3333-8498",
                LocalDate.of(2001, 5, 18),
                "M",
                LocalDate.of(2023, 4, 20),
                "경기도 성남시",
                "010518-3027485");

        // test5 계정 생성: 선임, 경영지원
        createTestUser("주영찬", "dudcks7624@gmail.com", "1234", 4L, 1L, DEFAULT_PROFILE_IMAGE_URL + "68",
                "010-9382-8512",
                LocalDate.of(1999, 6, 8),
                "M",
                LocalDate.of(2025, 2, 12),
                "경기도 남양주",
                "990608-1048372");
    }

    private void createDepartment(Long id, String name, String color, String imageUrl) {
        if (departmentRepository.findById(id).isEmpty()) {
            Department department = Department.builder()
                    .departmentId(id)
                    .name(name)
                    .departmentColor(color)
                    .imageUrl(imageUrl)
                    .build();
            departmentRepository.save(department);
            log.info("Department {} created.", name);
        } else {
            log.info("Department {} already exists. Skipping creation.", name);
        }
    }

    private void createPosition(Long id, String name) {
        if (positionRepository.findById(id).isEmpty()) {
            Position position = Position.builder()
                    .positionId(id)
                    .positionName(name)
                    .build();
            positionRepository.save(position);
            log.info("Position {} created.", name);
        } else {
            log.info("Position {} already exists. Skipping creation.", name);
        }
    }

    private void createTestUser(String userName, String email, String password, Long positionId, Long departmentId, String profileImageUrl, String phone,
                                LocalDate birthDate,
                                String gender,
                                LocalDate hireDate,
                                String address,
                                String residentRegNo) {
        if (userRepository.findByEmail(email).isEmpty()) {
            log.info("Creating test user: {}", email);

            Department department = Department.builder().departmentId(departmentId).build();
            Position position = Position.builder().positionId(positionId).build();

            String hrRole = (positionId == 1L || positionId == 2L) ? "Y" : "N";

            User testUser = User.builder()
                    .userName(userName)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .phone(phone)
                    .residentRegNo(residentRegNo)
                    .birthDate(birthDate)
                    .gender(gender)
                    .createdAt(LocalDateTime.now())
                    .hireDate(hireDate)
                    .address(address)
                    .profileImage(profileImageUrl)
                    .retireDate(null)
                    .activate("Y")
                    .department(department)
                    .position(position)
                    .hrRole(hrRole)
                    .build();

            userRepository.save(testUser);
            log.info("Test user {} created successfully.", email);
        } else {
            log.info("Test user {} already exists. Skipping creation.", email);
        }
    }
}