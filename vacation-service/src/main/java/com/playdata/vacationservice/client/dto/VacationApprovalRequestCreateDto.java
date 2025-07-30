package com.playdata.vacationservice.client.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@ToString
public class VacationApprovalRequestCreateDto {
    @NotNull(message = "요청 타입은 필수입니다.")
    private String requestType; // RequestType enum 대신 String으로 변경

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
