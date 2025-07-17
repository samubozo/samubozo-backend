package com.playdata.hrservice;

import com.playdata.hrservice.hr.dto.UserResDto;
import com.playdata.hrservice.hr.entity.Department;
import com.playdata.hrservice.hr.entity.Position;
import com.playdata.hrservice.hr.entity.User;
import com.playdata.hrservice.hr.repository.DepartmentRepository;
import com.playdata.hrservice.hr.repository.PositionRepository;
import com.playdata.hrservice.hr.repository.UserRepository;
import com.playdata.hrservice.hr.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled
@SpringBootTest
@Transactional
class HrServiceApplicationTests {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PositionRepository positionRepository;

    private Department dept1;
    private Department dept2;
    private Position pos1;
    private Position pos2;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        departmentRepository.deleteAll();
        positionRepository.deleteAll();

        dept1 = departmentRepository.save(Department.builder().name("경영지원").departmentColor("#FFAB91").build());
        dept2 = departmentRepository.save(Department.builder().name("인사팀").departmentColor("#B39DDB").build());

        pos1 = positionRepository.save(Position.builder().positionName("사장").hrRole("Y").build());
        pos2 = positionRepository.save(Position.builder().positionName("부장").hrRole("Y").build());

        User user1 = User.builder()
                .userName("홍길동")
                .email("hong@test.com")
                .password("password123")
                .phone("010-1111-2222")
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender("M")
                .createdAt(LocalDateTime.now())
                .hireDate(LocalDate.of(2020, 3, 1))
                .address("서울시 강남구")
                .profileImage("image1.jpg")
                .activate("Y")
                .department(dept1)
                .position(pos1)
                .build();

        User user2 = User.builder()
                .userName("김철수")
                .email("kim@test.com")
                .password("password123")
                .phone("010-3333-4444")
                .birthDate(LocalDate.of(1992, 5, 10))
                .gender("M")
                .createdAt(LocalDateTime.now())
                .hireDate(LocalDate.of(2021, 7, 15))
                .address("서울시 서초구")
                .profileImage("image2.jpg")
                .activate("Y")
                .department(dept2)
                .position(pos2)
                .build();

        User user3 = User.builder()
                .userName("이영희")
                .email("lee@test.com")
                .password("password123")
                .phone("010-5555-6666")
                .birthDate(LocalDate.of(1995, 8, 20))
                .gender("F")
                .createdAt(LocalDateTime.now())
                .hireDate(LocalDate.of(2022, 1, 1))
                .address("경기도 성남시")
                .profileImage("image3.jpg")
                .activate("Y")
                .department(dept1)
                .position(pos2)
                .build();

        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);
    }

    @Test
    void contextLoads() {
    }

    @Test
    void searchUsersByDepartmentName() {
        // 검색 조건이 있을 경우 (부서명), 페이징 없이 List 반환
        List<UserResDto> users = (List<UserResDto>) userService.searchUsers(null, "경영지원", null, null);
        assertThat(users).hasSize(2);
        assertThat(users).extracting(UserResDto::getUserName).containsExactlyInAnyOrder("홍길동", "이영희");
    }

    @Test
    void searchUsersByUserName() {
        // 검색 조건이 있을 경우 (이름), 페이징 없이 List 반환
        List<UserResDto> users = (List<UserResDto>) userService.searchUsers("홍길동", null, null, null);
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUserName()).isEqualTo("홍길동");
    }

    @Test
    void searchUsersByUserNameAndDepartmentName() {
        // 검색 조건이 있을 경우 (이름과 부서명), 페이징 없이 List 반환
        List<UserResDto> users = (List<UserResDto>) userService.searchUsers("이영희", "경영지원", null, null);
        assertThat(users).hasSize(1);
        assertThat(users.get(0).getUserName()).isEqualTo("이영희");
        assertThat(users.get(0).getDepartment().getName()).isEqualTo("경영지원");
    }

    @Test
    void searchUsersNoCriteria() {
        // 검색 조건이 없을 경우, 페이징 적용하여 Page 반환
        Pageable pageable = PageRequest.of(0, 10);
        Page<UserResDto> users = (Page<UserResDto>) userService.searchUsers(null, null, null, pageable);
        assertThat(users.getTotalElements()).isEqualTo(3);
        assertThat(users.getContent()).hasSize(3);
    }

    @Test
    void searchUsersNotFound() {
        // 검색 조건이 있을 경우 (존재하지 않는 부서명), 페이징 없이 List 반환
        List<UserResDto> users = (List<UserResDto>) userService.searchUsers(null, "없는부서", null, null);
        assertThat(users).isEmpty();
    }
}