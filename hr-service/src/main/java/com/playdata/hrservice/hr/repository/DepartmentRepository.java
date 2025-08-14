package com.playdata.hrservice.hr.repository;

import com.playdata.hrservice.hr.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    Department findByName(String departmentName);
    boolean existsByName(String name);
    boolean existsByDepartmentColor(String departmentColor);

}
