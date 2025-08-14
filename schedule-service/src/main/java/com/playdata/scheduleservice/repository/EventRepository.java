package com.playdata.scheduleservice.repository;

import com.playdata.scheduleservice.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    // 월별 일정 조회 (개인 및 그룹 일정 포함)
    @Query("SELECT e FROM Event e WHERE " +
            "(e.ownerEmployeeNo = :employeeNo OR e.category.departmentId = :departmentId) AND " +
            "((FUNCTION('YEAR', e.startDate) = :year AND FUNCTION('MONTH', e.startDate) = :month) OR " +
            " (FUNCTION('YEAR', e.endDate) = :year AND FUNCTION('MONTH', e.endDate) = :month) OR " +
            " (FUNCTION('YEAR', e.startDate) < :year AND FUNCTION('YEAR', e.endDate) > :year) OR " +
            " (FUNCTION('YEAR', e.startDate) = :year AND FUNCTION('MONTH', e.startDate) < :month AND FUNCTION('YEAR', e.endDate) = :year AND FUNCTION('MONTH', e.endDate) > :month) OR " +
            " (FUNCTION('YEAR', e.startDate) = :year AND FUNCTION('MONTH', e.startDate) < :month AND FUNCTION('YEAR', e.endDate) > :year) OR " +
            " (FUNCTION('YEAR', e.startDate) < :year AND FUNCTION('YEAR', e.endDate) = :year AND FUNCTION('MONTH', e.endDate) > :month)) ")
    List<Event> findMonthlyEvents(
            @Param("employeeNo") Long employeeNo,
            @Param("departmentId") Long departmentId,
            @Param("year") int year,
            @Param("month") int month);

    // 검색 기능 (제목, 내용, 메모에서 키워드 검색 및 카테고리 필터링)
    @Query("SELECT e FROM Event e WHERE " +
            "(e.ownerEmployeeNo = :employeeNo OR e.category.departmentId = :departmentId) AND " +
            "(:keyword IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(e.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:categoryId IS NULL OR e.category.id = :categoryId) AND " +
            "(:startDate IS NULL OR e.startDate >= :startDate) AND " +
            "(:endDate IS NULL OR e.endDate <= :endDate)")
    List<Event> searchEvents(
            @Param("employeeNo") Long employeeNo,
            @Param("departmentId") Long departmentId,
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    // 특정 카테고리에 속한 일정 조회 (카테고리 삭제 시 유효성 검사용)
    List<Event> findByCategoryId(Long categoryId);

    // 특정 사용자의 모든 일정 조회 (사용자 삭제 시 유효성 검사용)
    List<Event> findByOwnerEmployeeNo(Long ownerEmployeeNo);

    // 특정 부서의 모든 그룹 일정 조회 (부서 삭제 시 유효성 검사용)
    @Query("SELECT e FROM Event e WHERE e.category.type = 'GROUP' AND e.category.departmentId = :departmentId")
    List<Event> findGroupEventsByDepartmentId(@Param("departmentId") Long departmentId);

    // isAllDay가 true인 모든 일정 조회
    @Query("SELECT e FROM Event e WHERE e.isAllDay = true AND (e.ownerEmployeeNo = :employeeNo OR e.category.departmentId = :departmentId)")
    List<Event> findIsAllDayEvents(@Param("employeeNo") Long employeeNo, @Param("departmentId") Long departmentId);

}