package com.playdata.certificateservice.dto;


import com.playdata.certificateservice.entity.Status;
import com.playdata.certificateservice.entity.Type;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateResDto {
    private Long certificateId;
    private Long employeeNo;
    private Type type;
    private LocalDate requestDate;
    private LocalDate approveDate;
    private LocalDateTime processedAt;
    private Status status;
    private String purpose;
    private String applicantName;
    private String departmentName;
    private String approverName;
    private String rejectComment;
}
