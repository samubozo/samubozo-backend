package com.playdata.attendanceservice.workstatus.controller;

import com.playdata.attendanceservice.workstatus.service.WorkStatusService;
import com.playdata.attendanceservice.client.dto.VacationWorkStatusRequestDto;
import com.playdata.attendanceservice.common.dto.CommonResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/attendances/internal/work-status")
@RequiredArgsConstructor
public class WorkStatusController {

    private final WorkStatusService workStatusService;

    /**
     * API 요청 성공 시 공통 응답 객체를 생성하여 반환하는 헬퍼 메소드입니다.
     */
    private <T> ResponseEntity<CommonResDto<T>> buildSuccessResponse(T data, String message) {
        CommonResDto<T> resDto = new CommonResDto<>(HttpStatus.OK, message, data);
        return ResponseEntity.ok(resDto);
    }

    /**
     * API 요청 성공 시 공통 응답 객체를 생성하여 반환하는 헬퍼 메소드입니다.
     * HTTP 상태 코드를 직접 지정할 수 있습니다.
     */
    private <T> ResponseEntity<CommonResDto<T>> buildSuccessResponse(HttpStatus status, T data, String message) {
        CommonResDto<T> resDto = new CommonResDto<>(status, message, data);
        return ResponseEntity.status(status).body(resDto);
    }

    @PostMapping("/vacation")
    public ResponseEntity<CommonResDto<Void>> createWorkStatusForVacation(@RequestBody VacationWorkStatusRequestDto requestDto) {
        workStatusService.createWorkStatusForVacation(requestDto);
        return buildSuccessResponse(HttpStatus.OK, null, "휴가 WorkStatus 생성 성공");
    }
}
