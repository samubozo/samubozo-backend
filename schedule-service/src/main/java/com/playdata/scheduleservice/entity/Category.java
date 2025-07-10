package com.playdata.scheduleservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 7) // e.g., #RRGGBB
    private String color;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoryType type; // PERSONAL, GROUP

    @Column(name = "owner_employee_no")
    private Long ownerEmployeeNo; // For PERSONAL type

    @Column(name = "department_id")
    private Long departmentId; // For GROUP type

    @Column(name = "is_checked", nullable = false)
    private Boolean isChecked; // For frontend checkbox state

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Enum for CategoryType
    public enum CategoryType {
        PERSONAL, GROUP
    }
}