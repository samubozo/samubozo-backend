package com.playdata.payrollservice.payroll.repository;



import com.playdata.payrollservice.payroll.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    Optional<User> findBySocialProviderAndSocialId(String socialId, String socialProvider);

}
