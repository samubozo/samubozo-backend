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

    @Override
    public void run(String... args) throws Exception {
        // 테스트 계정 생성 (test1@example.com, 비밀번호: 1234)
        if (userRepository.findByEmail("test1@example.com").isEmpty()) {
            log.info("Creating test user: test1@example.com");

            // 기본 부서 및 직책 조회 (data.sql에 이미 있다고 가정)
            Department defaultDepartment = departmentRepository.findById(1L)
                    .orElseThrow(() -> new RuntimeException("Default Department not found"));
            Position defaultPosition = positionRepository.findById(1L)
                    .orElseThrow(() -> new RuntimeException("Default Position not found"));

            User testUser = User.builder()
                    .userName("test1")
                    .email("test1@example.com")
                    .password(passwordEncoder.encode("1234"))
                    .phone("010-1234-5678")
                    .birthDate(LocalDate.of(1990, 1, 1))
                    .gender("M")
                    .createdAt(LocalDateTime.now())
                    .hireDate(LocalDate.of(2023, 1, 1))
                    .address("서울시 강남구")
                    .profileImage(null)
                    .retireDate(null)
                    .activate("Y")
                    .department(defaultDepartment)
                    .position(defaultPosition)
                    .build();

            userRepository.save(testUser);
            log.info("Test user test1@example.com created successfully.");
        }
    }
}
