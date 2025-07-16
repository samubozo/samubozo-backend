package com.playdata.certificateservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;

import static com.playdata.certificateservice.entity.Status.REQUESTED;


@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "tbl_certificates")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "certificate_id", nullable = false)
    private Long certificateId;

    @Column(name = "employee_no", nullable = false)
    private Long employeeNo;

    @Enumerated(EnumType.STRING)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = REQUESTED;

    @Column(columnDefinition = "TEXT")
    private String purpose;

    @Column(name = "request_date")
    private LocalDate requestDate;

    @Column(name = "approve_date")
    private LocalDate approveDate;

}
