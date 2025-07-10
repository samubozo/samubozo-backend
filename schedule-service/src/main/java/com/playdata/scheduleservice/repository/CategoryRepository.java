package com.playdata.scheduleservice.repository;

import com.playdata.scheduleservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByOwnerEmployeeNo(Long ownerEmployeeNo);
    List<Category> findByDepartmentId(Long departmentId);
    Optional<Category> findByIdAndOwnerEmployeeNo(Long id, Long ownerEmployeeNo);
    Optional<Category> findByIdAndDepartmentId(Long id, Long departmentId);
}