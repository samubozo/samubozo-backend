package com.playdata.approvalservice.approval.dto;

import com.playdata.approvalservice.approval.entity.RequestType;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/**
 * 휴가 결재 요청 생성을 위한 DTO입니다.
 * 휴가 관련 필드만 포함합니다.
 */
@Getter
@Setter
@Builder
@ToString
public class VacationApprovalRequestCreateDto {
    @NotNull(message = "요청 타입은 필수입니다.")
    private RequestType requestType; // 항상 VACATION

    @NotNull(message = "신청자 ID는 필수입니다.")
    private Long applicantId;

    @NotNull(message = "제목은 필수입니다.")
    private String title;

    private String reason; // 요청 사유

    @NotNull(message = "휴가 ID는 필수입니다.")
    private Long vacationsId; // 휴가 서비스의 휴가 ID

    @NotNull(message = "휴가 타입은 필수입니다.")
    private String vacationType; // 연차, 반차, 조퇴 등

    @NotNull(message = "시작일은 필수입니다.")
    private LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private LocalDate endDate;
}
