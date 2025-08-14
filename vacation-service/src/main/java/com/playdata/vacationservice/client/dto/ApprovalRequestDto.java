package com.playdata.vacationservice.client.dto;

import com.playdata.vacationservice.vacation.entity.RequestType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * 결재 서비스에 휴가 결재 생성을 요청하기 위한 DTO 입니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ApprovalRequestDto {

    @NotNull(message = "요청 타입은 필수입니다.")
    private RequestType requestType; // 항상 VACATION

    @NotNull(message = "신청자 ID는 필수입니다.")
    private Long applicantId;

    @NotNull(message = "제목은 필수입니다.")
    private String title;

    private String reason;

    @NotNull(message = "휴가 ID는 필수입니다.")
    private Long vacationsId;

    @NotNull(message = "휴가 타입은 필수입니다.")
    private String vacationType;

    @NotNull(message = "시작일은 필수입니다.")
    private java.time.LocalDate startDate;

    @NotNull(message = "종료일은 필수입니다.")
    private java.time.LocalDate endDate;
}