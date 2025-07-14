package com.playdata.hrservice.hr.service;

import com.playdata.hrservice.common.configs.AwsS3Config;
import com.playdata.hrservice.hr.dto.DepartmentReqDto;
import com.playdata.hrservice.hr.dto.DepartmentResDto;
import com.playdata.hrservice.hr.entity.Department;
import com.playdata.hrservice.hr.repository.DepartmentRepository;
import com.playdata.hrservice.hr.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository; // 추가
    private final AwsS3Config awsS3Config;

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

        String imageUrl = null;
        MultipartFile departmentImage = dto.getDepartmentImage();
        if (departmentImage != null && !departmentImage.isEmpty()) {
            try {
                String originalFilename = departmentImage.getOriginalFilename();
                String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFilename;
                imageUrl = awsS3Config.uploadToS3Bucket(departmentImage.getBytes(), uniqueFileName);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload department image to S3", e);
            }
        }

        Department department = Department.builder()
                .name(dto.getName())
                .departmentColor(dto.getDepartmentColor())
                .imageUrl(imageUrl)
                .build();
        departmentRepository.save(department);
    }

    // 부서 수정
    @Transactional
    public void updateDepartment(Long departmentId, DepartmentReqDto dto) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with ID: " + departmentId));

        // 부서명 중복 체크 (자신 제외)
        if (departmentRepository.existsByName(dto.getName()) && !department.getName().equals(dto.getName())) {
            throw new IllegalArgumentException("이미 사용 중인 부서명입니다.");
        }

        // 컬러 중복 체크 (자신 제외)
        if (departmentRepository.existsByDepartmentColor(dto.getDepartmentColor()) && !department.getDepartmentColor().equals(dto.getDepartmentColor())) {
            throw new IllegalArgumentException("이미 사용 중인 부서 색상입니다.");
        }

        String imageUrl = department.getImageUrl();
        MultipartFile departmentImage = dto.getDepartmentImage();
        if (departmentImage != null && !departmentImage.isEmpty()) {
            // 기존 이미지 삭제
            if (imageUrl != null && !imageUrl.isBlank()) {
                try {
                    awsS3Config.deleteFromS3Bucket(imageUrl);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to delete old department image from S3", e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            // 새 이미지 업로드
            try {
                String originalFilename = departmentImage.getOriginalFilename();
                String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFilename;
                imageUrl = awsS3Config.uploadToS3Bucket(departmentImage.getBytes(), uniqueFileName);
            } catch (IOException e) {
                throw new RuntimeException("Failed to upload new department image to S3", e);
            }
        } else { // 이미지를 null로 보내면 기존 이미지 삭제
            // 이 부분은 기존 이미지를 유지하도록 변경되므로 삭제
        }

        department.setName(dto.getName());
        department.setDepartmentColor(dto.getDepartmentColor());
        department.setImageUrl(imageUrl);

        departmentRepository.save(department);
    }

    // 부서 삭제
    @Transactional
    public void deleteDepartment(Long departmentId) {
        Department department = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new EntityNotFoundException("Department not found with ID: " + departmentId));

        // 해당 부서에 속한 직원이 있는지 확인
        if (userRepository.existsByDepartmentDepartmentId(departmentId)) {
            throw new IllegalArgumentException("해당 부서에 속한 직원이 존재하여 삭제할 수 없습니다.");
        }

        // 이미지 파일이 있다면 S3에서 삭제
        if (department.getImageUrl() != null && !department.getImageUrl().isBlank()) {
            try {
                awsS3Config.deleteFromS3Bucket(department.getImageUrl());
            } catch (Exception e) {
                throw new RuntimeException("Failed to delete department image from S3", e);
            }
        }

        departmentRepository.delete(department);
    }

}
