package com.playdata.authservice.auth.entity;



import com.playdata.authservice.auth.dto.UserResDto;
import com.playdata.authservice.common.auth.Role;
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
    @Column(name = "user_id")
    private Long userId;

    @Column(length = 50, nullable = false)
    private String name;

    @Column( length = 255, nullable = true)
    private String password;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(nullable = true)
    private String address;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role =  Role.USER;

    @Enumerated(EnumType.STRING)
    private UserStatus status = UserStatus.ACTIVE;

    public boolean isAdmin() {
        return this.role == Role.ADMIN;
    }

    @Column(length = 20, name = "phone", nullable = true)
    private String phone;

    @Column(name = "birth_date", nullable = true)
    private LocalDate birthDate;

    @Column(name = "registered_at", nullable = false)
    private LocalDateTime registeredAt;


    @Column
    private String socialId;

    @Column
    private String profileImage;

    @Column
    private String socialProvider;


    public UserResDto fromEntity() {
        return UserResDto.builder()
                .userid(userId)
                .name(name)
                .email(email)
                .role(role)
                .address(address)
                .profileImage(profileImage)
                .socialProvider(socialProvider)
                .phone(phone)
                .birthdate(birthDate)
                .build();
    }

}






