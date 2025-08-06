package com.playdata.certificateservice.client.dto;

import com.playdata.certificateservice.client.dto.RequestType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/**
 * 증명서 결재 요청 생성을 위한 DTO입니다. (approval-service 호출용)
 * 증명서 관련 필드만 포함합니다.
 */
@Getter
@Setter
@Builder
@ToString
public class CertificateApprovalRequestCreateDto {
    @NotNull(message = "요청 타입은 필수입니다.")
    private RequestType requestType; // 항상 CERTIFICATE

    @NotNull(message = "신청자 ID는 필수입니다.")
    private Long applicantId;

    @NotNull(message = "제목은 필수입니다.")
    private String title;

    private String reason; // 요청 사유

    @NotNull(message = "증명서 ID는 필수입니다.")
    private Long certificateId; // 증명서 서비스의 증명서 ID

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDate endDate;
    private Type certificateType; // 추가: 증명서 유형 (EMPLOYMENT, CAREER 등)
}
