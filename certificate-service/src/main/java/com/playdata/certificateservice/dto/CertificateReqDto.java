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
public class CertificateReqDto {

    private Long certificateId;
    private Long employeeNo;
    private Type type;
    private Status status;
    private String purpose;
    private LocalDate requestDate;
    private LocalDate approveDate;

}
