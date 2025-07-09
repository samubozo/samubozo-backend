package com.playdata.hrservice.hr.service;

import com.playdata.hrservice.hr.dto.DepartmentReqDto;
import com.playdata.hrservice.hr.dto.DepartmentResDto;
import com.playdata.hrservice.hr.entity.Department;
import com.playdata.hrservice.hr.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<DepartmentResDto> getAllDepartments() {
        List<Department> departments = departmentRepository.findAll();
        return departments.stream()
                .map(DepartmentResDto::new)
                .collect(Collectors.toList());
    }

    // 부서 추가
    @Transactional
    public void createDepartment(DepartmentReqDto dto) {
        // 부서명 중복 체크
        if (departmentRepository.existsByName(dto.getName())) {
            throw new IllegalArgumentException("이미 사용 중인 부서명입니다.");
        }

        // 컬러 중복 체크
        if (departmentRepository.existsByDepartmentColor(dto.getDepartmentColor())) {
            throw new IllegalArgumentException("이미 사용 중인 부서 색상입니다.");
        }

        Department department = Department.builder()
                .name(dto.getName())
                .departmentColor(dto.getDepartmentColor())
                .build();
        departmentRepository.save(department);
    }

}
