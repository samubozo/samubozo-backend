package com.playdata.hrservice.hr.repository;

import com.playdata.hrservice.hr.entity.Department;
import com.playdata.hrservice.hr.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByName(String name);
    boolean existsByDepartmentColor(String departmentColor);


}
