package com.playdata.hrservice.hr.service;

import com.playdata.hrservice.hr.dto.DepartmentReqDto;
import com.playdata.hrservice.hr.dto.DepartmentResDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DepartmentService {
    List<DepartmentResDto> getAllDepartments();

    // 부서 추가
    @Transactional
    void createDepartment(DepartmentReqDto dto);

    // 부서 수정
    @Transactional
    void updateDepartment(Long departmentId, DepartmentReqDto dto);

    // 부서 삭제
    @Transactional
    void deleteDepartment(Long departmentId);
}
