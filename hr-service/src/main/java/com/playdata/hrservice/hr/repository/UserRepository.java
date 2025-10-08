package com.playdata.hrservice.hr.repository;


import com.playdata.hrservice.hr.dto.UserListResDto;
import com.playdata.hrservice.hr.dto.UserResDto;
import com.playdata.hrservice.hr.dto.UserSearchCondition;
import com.playdata.hrservice.hr.entity.User;
import org.jose4j.jwk.Use;
import org.springframework.data.domain.Page; // 추가
import org.springframework.data.domain.Pageable; // 추가
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmployeeNo(Long employeeNo);

    // 검색 조건이 있을 때 페이징 없이 전체 리스트를 위한 메서드 추가
    List<User> findByUserNameContaining(String userName);

    boolean existsByDepartmentDepartmentId(Long departmentId);

    List<User> findByEmployeeNoIn(List<Long> employeeNos);

    @Query("SELECT u FROM User u WHERE YEAR(u.hireDate) = :year AND MONTH(u.hireDate) = :month")
    List<User> findUsersWithHireDateInMonth(int year, int month);

    // listUsers를 위해 Fetch Join을 사용하여 N+1 문제를 해결하는 메소드
    @Query("select u from User u join fetch u.department d join fetch u.position p")
    Page<User> findAllWithDetails(Pageable pageable);
}
