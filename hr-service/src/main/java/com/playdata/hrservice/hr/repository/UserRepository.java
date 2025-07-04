package com.playdata.hrservice.hr.repository;


import com.playdata.hrservice.hr.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<User> findByEmployeeNo(Long employeeNo);
}
