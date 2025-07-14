package com.playdata.certificateservice.dto;


import com.playdata.certificateservice.entity.Status;
import com.playdata.certificateservice.entity.Type;
import lombok.*;

import java.time.LocalDate;

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
    private Status status;
    private String purpose;
}
