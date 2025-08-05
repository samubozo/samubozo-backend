package com.playdata.approvalservice.client.dto;

import com.playdata.approvalservice.approval.entity.Type;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@Setter
@ToString
public class Certificate {
    private Long certificateId;
    private Long employeeNo;
    private Type type;
    private LocalDate requestDate;
    private LocalDate approveDate;
    private String status; // Status enum 대신 String으로
    private String purpose;
    private Long approvalRequestId;
}
