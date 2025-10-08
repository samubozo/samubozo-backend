package com.playdata.hrservice.hr.entity;


import com.playdata.hrservice.common.entity.Address;
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

    @Embedded
    private Address address;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(length = 255)
    private String profileImage;

    @Column(name = "bank_name", length = 50)
    private String bankName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "account_holder", length = 50)
    private String accountHolder;

    @Column(name = "retire_date")
    private LocalDate retireDate;

    @Column(length = 4, nullable = false)
    @Builder.Default
    private String activate = "Y";

    @Column(name = "hr_role", length = 4, nullable = false)
    @Builder.Default
    private String hrRole = "N";

    // === 연관관계 ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id", nullable = false)
    private Position position;

    public UserResDto fromEntity() {
        String displayAddress = "";
        if (address != null) {
            // [수정] zipCode를 제외하고 city와 street만 조합합니다.
            displayAddress = (address.getCity() != null ? address.getCity() : "")
                    + " " + (address.getStreet() != null ? address.getStreet() : "");
            displayAddress = displayAddress.trim();
        }
        return UserResDto.builder()
                .employeeNo(employeeNo)
                .userName(userName)
                .email(email)
                .externalEmail(externalEmail)
                .password(password) // [보안 권장] 상세 정보 조회 시에도 password는 제외하는 것이 좋습니다.
                .gender(gender)
                .residentRegNo(residentRegNo) // [보안 권장] 민강 정보이므로 꼭 필요한 경우가 아니면 제외하는 것이 좋습니다.
                .department(department != null ? new DepartmentResDto(department) : null)
                .positionId(position != null ? position.getPositionId() : null)
                .positionName(position != null ? position.getPositionName() : null)
                .address(displayAddress) // [수정] 조합된 주소 문자열을 한 번에 설정
                .remarks(remarks)
                .profileImage(profileImage)
                .phone(phone)
                .birthDate(birthDate)
                .hireDate(hireDate)
                .retireDate(retireDate)
                .bankName(bankName)
                .accountNumber(accountNumber)
                .accountHolder(accountHolder)
                .activate(activate)
                .hrRole(hrRole)
                .build();
    }

    public UserLoginFeignResDto toUserLoginFeignResDto() {
        return UserLoginFeignResDto.builder()
                .employeeNo(employeeNo)
                .userName(userName)
                .email(email)
                .password(password)
                .activate(activate)
                .hrRole(hrRole)
                .build();
    }

    /**
     * Feign 통신을 위한 UserFeignResDto로 변환합니다.
     * Address 객체를 '도시 거리' 형식의 주소 문자열로 조합합니다.
     */

    public UserFeignResDto toUserFeignResDto() {
        String displayAddress = "";
        if (address != null) {
            displayAddress = (address.getCity() != null ? address.getCity() : "")
                    + " " + (address.getStreet() != null ? address.getStreet() : "");
            displayAddress = displayAddress.trim();
        }

        return UserFeignResDto.builder()
                .employeeNo(employeeNo)
                .userName(userName)
                .residentRegNo(residentRegNo)
                .email(email)
                .externalEmail(externalEmail)
                .password(password)
                .gender(gender)
                .department(department != null ? new DepartmentResDto(department) : null)
                .positionId(position != null ? position.getPositionId() : null)
                .positionName(position != null ? position.getPositionName() : null)
                .address(displayAddress) // [수정] 조합된 주소 문자열을 한 번에 설정
                .remarks(remarks)
                .profileImage(profileImage)
                .phone(phone)
                .birthDate(birthDate)
                .hireDate(hireDate)
                .retireDate(retireDate)
                .activate(activate)
                .hrRole(hrRole)
                .build();
    }

}





