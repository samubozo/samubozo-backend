package com.playdata.attendanceservice.attendance.absence.controller;

import com.playdata.attendanceservice.attendance.absence.dto.request.AbsenceRequestDto;
import com.playdata.attendanceservice.attendance.absence.dto.request.AbsenceUpdateRequestDto;
import com.playdata.attendanceservice.attendance.absence.dto.response.AbsenceResponseDto;
import com.playdata.attendanceservice.attendance.absence.service.AbsenceService;
import com.playdata.attendanceservice.common.auth.TokenUserInfo;
import com.playdata.attendanceservice.common.dto.CommonResDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/attendance/absence")
@RequiredArgsConstructor
public class AbsenceController {

    private final AbsenceService absenceService;

    // 부재 등록
    @PostMapping
    public ResponseEntity<CommonResDto<Void>> createAbsence(
            @AuthenticationPrincipal TokenUserInfo userInfo,
            @RequestBody AbsenceRequestDto request) {
        if (userInfo == null) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다.");
        }
        try {
            absenceService.createAbsence(String.valueOf(userInfo.getEmployeeNo()), request);
            return buildSuccessResponse(null, "부재 정보가 성공적으로 등록되었습니다.");
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "잘못된 요청: " + e.getMessage());
        } catch (Exception e) {
            // TODO: logger로 에러 로그 남기기
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "부재 등록 중 오류 발생: " + e.getMessage());
        }
    }

    // 내 부재 목록 조회
    @GetMapping("/my")
    public ResponseEntity<CommonResDto<List<AbsenceResponseDto>>> getMyAbsences(
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        if (userInfo == null) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다.");
        }
        try {
            List<AbsenceResponseDto> absences = absenceService.getAbsencesByUserId(String.valueOf(userInfo.getEmployeeNo()));
            return buildSuccessResponse(absences, "조회 성공");
        } catch (Exception e) {
            // TODO: logger로 에러 로그 남기기
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "부재 내역 조회 중 오류 발생: " + e.getMessage());
        }
    }

    // 단일 부재 상세 조회
    @GetMapping("/{absenceId}")
    public ResponseEntity<CommonResDto<AbsenceResponseDto>> getAbsenceById(
            @PathVariable Long absenceId) {
        try {
            AbsenceResponseDto absence = absenceService.getAbsenceById(absenceId);
            if (absence == null) {
                return buildErrorResponse(HttpStatus.NOT_FOUND, "해당 부재 내역을 찾을 수 없습니다.");
            }
            return buildSuccessResponse(absence, "상세 조회 성공");
        } catch (Exception e) {
            // TODO: logger로 에러 로그 남기기
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "상세 조회 중 오류 발생: " + e.getMessage());
        }
    }

    // 부재 수정
    @PutMapping("/{absenceId}")
    public ResponseEntity<CommonResDto<AbsenceResponseDto>> updateAbsence(
            @PathVariable Long absenceId,
            @RequestBody AbsenceUpdateRequestDto request) {
        try {
            AbsenceResponseDto updatedAbsence = absenceService.updateAbsence(absenceId, request);
            return buildSuccessResponse(updatedAbsence, "부재 정보가 성공적으로 업데이트되었습니다.");
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            // TODO: logger로 에러 로그 남기기
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "부재 정보 업데이트 중 오류 발생: " + e.getMessage());
        }
    }

    // 부재 삭제
    @DeleteMapping("/{absenceId}")
    public ResponseEntity<CommonResDto<Void>> deleteAbsence(
            @PathVariable Long absenceId) {
        try {
            absenceService.deleteAbsence(absenceId);
            return buildSuccessResponse(null, "부재 정보가 성공적으로 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            // TODO: logger로 에러 로그 남기기
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "부재 정보 삭제 중 오류 발생: " + e.getMessage());
        }
    }

    // 공통 응답 빌더
    private <T> ResponseEntity<CommonResDto<T>> buildSuccessResponse(T data, String message) {
        CommonResDto<T> resDto = new CommonResDto<>(HttpStatus.OK, message, data);
        return ResponseEntity.ok(resDto);
    }

    private <T> ResponseEntity<CommonResDto<T>> buildErrorResponse(HttpStatus status, String message) {
        CommonResDto<T> resDto = new CommonResDto<>(status, message, null);
        return ResponseEntity.status(status).body(resDto);
    }
}