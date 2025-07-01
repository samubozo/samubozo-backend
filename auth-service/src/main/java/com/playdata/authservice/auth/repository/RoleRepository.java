package com.playdata.authservice.auth.repository;

import com.playdata.hrservice.hr.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}
