package com.playdata.hrservice.hr.service;

import com.playdata.hrservice.hr.dto.DepartmentResDto;
import com.playdata.hrservice.hr.entity.Department;
import com.playdata.hrservice.hr.repository.DepartmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
