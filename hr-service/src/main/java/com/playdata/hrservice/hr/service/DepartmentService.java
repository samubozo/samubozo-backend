package com.playdata.hrservice.hr.service;

import com.playdata.hrservice.hr.dto.DepartmentReqDto;
import com.playdata.hrservice.hr.dto.DepartmentResDto;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface DepartmentService {
    List<DepartmentResDto> getAllDepartments();

    @Transactional
    void createDepartment(DepartmentReqDto dto);

    @Transactional
    void updateDepartment(Long departmentId, DepartmentReqDto dto);

    @Transactional
    void deleteDepartment(Long departmentId);
}
