package com.playdata.attendanceservice.attendance.repository;


import com.playdata.attendanceservice.attendance.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findBySocialProviderAndSocialId(String socialId, String socialProvider);

}
