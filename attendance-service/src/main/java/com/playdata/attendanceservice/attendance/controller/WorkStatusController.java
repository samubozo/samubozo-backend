package com.playdata.attendanceservice.attendance.controller;

import com.playdata.attendanceservice.attendance.service.WorkStatusService;
import com.playdata.attendanceservice.client.dto.VacationWorkStatusRequestDto;
import lombok.RequiredArgsConstructor;
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

    @PostMapping("/vacation")
    public ResponseEntity<Void> createWorkStatusForVacation(@RequestBody VacationWorkStatusRequestDto requestDto) {
        workStatusService.createWorkStatusForVacation(requestDto);
        return ResponseEntity.ok().build();
    }
}
