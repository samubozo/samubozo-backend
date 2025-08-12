package com.playdata.hrservice.hr.repository;


import com.playdata.hrservice.hr.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {


    Optional<User> findByEmail(String email);

    Optional<User> findByEmployeeNo(Long employeeNo);

    @Query(
            value = """
    SELECT u.*
    FROM tbl_users u
    WHERE EXISTS (
      SELECT 1
      FROM payrolls p
      WHERE p.user_id = u.employee_no      
        AND p.pay_year = :year
        AND p.pay_month = :month
    )
    """,
            countQuery = """
    SELECT COUNT(*)
    FROM tbl_users u
    WHERE EXISTS (
      SELECT 1
      FROM payrolls p
      WHERE p.user_id = u.employee_no      
        AND p.pay_year = :year
        AND p.pay_month = :month
    )
    """,
            nativeQuery = true
    )
    Page<User> findUsersHavingPayrollInYearMonth(
            @Param("year") int year,
            @Param("month") int month,
            Pageable pageable
    );

    // 검색 조건이 있을 때 페이징 없이 전체 리스트를 위한 메서드 추가
    List<User> findByUserNameContaining(String userName);

    @Query("SELECT u FROM User u JOIN u.department d WHERE d.name LIKE %:departmentName%")
    List<User> findByDepartmentNameContaining(String departmentName); // 메서드 이름 변경

    @Query("SELECT u FROM User u JOIN u.department d WHERE u.userName LIKE %:userName% AND d.name LIKE %:departmentName%")
    List<User> findByUserNameContainingAndDepartmentNameContaining(String userName, String departmentName); // 메서드 이름 변경

    // hrRole 검색 추가
    List<User> findByHrRole(String hrRole);
    List<User> findByUserNameContainingAndHrRole(String userName, String hrRole);
    @Query("SELECT u FROM User u JOIN u.department d JOIN u.position p WHERE d.name LIKE %:departmentName% AND u.hrRole = :hrRole")
    List<User> findByDepartmentNameContainingAndHrRole(String departmentName, String hrRole);
    @Query("SELECT u FROM User u JOIN u.department d JOIN u.position p WHERE u.userName LIKE %:userName% AND d.name LIKE %:departmentName% AND u.hrRole = :hrRole")
    List<User> findByUserNameContainingAndDepartmentNameContainingAndHrRole(String userName, String departmentName, String hrRole);

    boolean existsByDepartmentDepartmentId(Long departmentId);

    List<User> findByEmployeeNoIn(List<Long> employeeNos);

    @Query("SELECT u FROM User u WHERE YEAR(u.hireDate) = :year AND MONTH(u.hireDate) = :month")
    List<User> findUsersWithHireDateInMonth(int year, int month);
}
