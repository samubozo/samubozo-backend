package com.playdata.hrservice.hr.entity;


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

    @Column(length = 20, nullable = false)
    private String phone;

    @Column(name = "birth_date", nullable = false)
    private LocalDateTime birthDate;

    @Column(nullable = false, length = 1)
    private String gender;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(length = 255)
    private String profileImage;

    @Column(length = 4, nullable = false)
    private String activate = "Y";

    @Column(name = "department_id", nullable = false)
    private Long departmentId;

    @Column(name = "position_id", nullable = false)
    private Long positionId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    // === 연관관계 ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = false)
    private Position position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    public UserResDto fromEntity() {
        return UserResDto.builder()
                .employeeNo(employeeNo)
                .userName(userName)
                .email(email)
                .gender(gender)
                .roleId(role != null ? role.getRoleId() : null)
                .roleName(role != null ? role.getRoleName() : null)
                .departmentId(department != null ? department.getDepartmentId() : null)
                .departmentName(department != null ? department.getName() : null)
                .positionId(position != null ? position.getPositionId() : null)
                .positionName(position != null ? position.getPositionName() : null)
                .address(address)
                .profileImage(profileImage)
                .phone(phone)
                .birthDate(birthDate)
                .hireDate(hireDate)
                .activate(activate)
                .build();
    }

}






