package com.playdata.authservice.auth.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tbl_roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "role_name", length = 30, nullable = false)
    private String roleName; // ADMIN, HR, EMPLOYEE 등

    @Column(nullable = false)
    private int priority; // 권한 우선순위

}
