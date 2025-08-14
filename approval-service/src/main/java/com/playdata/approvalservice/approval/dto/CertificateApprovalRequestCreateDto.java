package com.playdata.approvalservice.approval.dto;

import com.playdata.approvalservice.approval.entity.Type;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 증명서 결재 요청 생성을 위한 DTO입니다.
 * 증명서 관련 필드만 포함합니다.
 */
@Getter
@Setter
@Builder
@ToString
public class CertificateApprovalRequestCreateDto {

    @NotNull(message = "제목은 필수입니다.")
    private String title;

    private String reason; // 요청 사유

    @NotNull(message = "증명서 ID는 필수입니다.")
    private Long certificateId; // 증명서 서비스의 증명서 ID

    @NotNull(message = "증명서 종류는 필수입니다.")
    private Type certificateType; // 증명서 종류


}
