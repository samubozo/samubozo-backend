package com.playdata.hrservice.hr.repository;

import com.playdata.hrservice.hr.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}
