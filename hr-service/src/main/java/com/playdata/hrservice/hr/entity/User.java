package com.playdata.hrservice.hr.entity;


import com.playdata.hrservice.hr.dto.DepartmentResDto;
import com.playdata.hrservice.hr.dto.UserFeignResDto;
import com.playdata.hrservice.hr.dto.UserLoginFeignResDto;
import com.playdata.hrservice.hr.dto.UserResDto;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tbl_users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_no",nullable = false)
    private Long employeeNo;

    @Column(name = "user_name",length = 50, nullable = false)
    private String userName;

    @Column(nullable = false, length = 255)
    private String password;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(name = "external_email", length = 100)
    private String externalEmail;

    @Column(name = "resident_reg_no", length = 100)
    private String residentRegNo;

    @Column(length = 20, nullable = false)
    private String phone;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 10)
    private String gender;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(length = 255)
    private String profileImage;

    @Column(name = "retire_date")
    private LocalDate retireDate;

    @Column(length = 4, nullable = false)
    @Builder.Default
    private String activate = "Y";

    // === 연관관계 ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = false)
    private Position position;

    public UserResDto fromEntity() {
        return UserResDto.builder()
                .employeeNo(employeeNo)
                .userName(userName)
                .email(email)
                .externalEmail(externalEmail)
                .password(password)
                .gender(gender)
                .department(department != null ? new DepartmentResDto(department) : null)
                .positionId(position != null ? position.getPositionId() : null)
                .positionName(position != null ? position.getPositionName() : null)
                .address(address)
                .remarks(remarks)
                .profileImage(profileImage)
                .phone(phone)
                .birthDate(birthDate)
                .hireDate(hireDate)
                .retireDate(retireDate)
                .activate(activate)
                .hrRole(position.getHrRole())
                .build();
    }

    public UserLoginFeignResDto toUserLoginFeignResDto() {
        return UserLoginFeignResDto.builder()
                .employeeNo(employeeNo)
                .username(userName)
                .email(email)
                .password(password)
                .activate(activate)
                .hrRole(position.getHrRole())
                .build();
    }

    public UserFeignResDto toUserFeignResDto() {
        return UserFeignResDto.builder()
                .employeeNo(employeeNo)
                .userName(userName)
                .email(email)
                .externalEmail(externalEmail)
                .password(password)
                .gender(gender)
                .department(department != null ? new DepartmentResDto(department) : null)
                .positionId(position != null ? position.getPositionId() : null)
                .positionName(position != null ? position.getPositionName() : null)
                .address(address)
                .remarks(remarks)
                .profileImage(profileImage)
                .phone(phone)
                .birthDate(birthDate)
                .hireDate(hireDate)
                .retireDate(retireDate)
                .activate(activate)
                .hrRole(position.getHrRole())
                .build();
    }

}





