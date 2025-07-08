package com.playdata.hrservice.hr.repository;


import com.playdata.hrservice.hr.entity.User;
import org.springframework.data.domain.Page; // 추가
import org.springframework.data.domain.Pageable; // 추가
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<User> findByEmployeeNo(Long employeeNo);

    // 사용자 검색을 위한 메서드 추가 (페이징 적용)
    Page<User> findByUserNameContaining(String userName, Pageable pageable);

    @Query("SELECT u FROM User u JOIN u.department d WHERE d.name LIKE %:departmentName%")
    Page<User> findByDepartmentNameContaining(String departmentName, Pageable pageable);

    @Query("SELECT u FROM User u JOIN u.department d WHERE u.userName LIKE %:userName% AND d.name LIKE %:departmentName%")
    Page<User> findByUserNameContainingAndDepartmentNameContaining(String userName, String departmentName, Pageable pageable);
}
