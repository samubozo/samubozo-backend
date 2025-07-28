package com.playdata.attendanceservice.attendance.absence.controller;

import com.playdata.attendanceservice.attendance.absence.dto.request.AbsenceRequestDto;
import com.playdata.attendanceservice.attendance.absence.dto.request.AbsenceUpdateRequestDto;
import com.playdata.attendanceservice.attendance.absence.dto.response.AbsenceResponseDto;
import com.playdata.attendanceservice.attendance.absence.entity.ApprovalStatus;
import com.playdata.attendanceservice.attendance.absence.service.AbsenceService;
import com.playdata.attendanceservice.common.auth.TokenUserInfo;
import com.playdata.attendanceservice.common.dto.CommonResDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/attendance/absence")
@RequiredArgsConstructor
@Slf4j
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
            log.info("부재 등록 요청: userId={}, type={}", userInfo.getEmployeeNo(), request.getType());

            absenceService.createAbsence(String.valueOf(userInfo.getEmployeeNo()), request);

            log.info("부재 등록 완료: userId={}", userInfo.getEmployeeNo());
            return buildSuccessResponse(null, "부재 정보가 성공적으로 등록되었습니다.");
        } catch (IllegalArgumentException e) {
            log.warn("부재 등록 실패 - 잘못된 요청: {}", e.getMessage());
            return buildErrorResponse(HttpStatus.BAD_REQUEST, "잘못된 요청: " + e.getMessage());
        } catch (Exception e) {
            log.error("부재 등록 중 오류 발생", e);
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
            log.info("내 부재 목록 조회: userId={}", userInfo.getEmployeeNo());

            List<AbsenceResponseDto> absences = absenceService.getAbsencesByUserId(String.valueOf(userInfo.getEmployeeNo()));

            log.info("내 부재 목록 조회 완료: count={}", absences.size());
            return buildSuccessResponse(absences, "조회 성공");
        } catch (Exception e) {
            log.error("부재 내역 조회 중 오류 발생", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "부재 내역 조회 중 오류 발생: " + e.getMessage());
        }
    }

    // 단일 부재 상세 조회
    @GetMapping("/{absenceId}")
    public ResponseEntity<CommonResDto<AbsenceResponseDto>> getAbsenceById(
            @PathVariable Long absenceId) {
        try {
            log.info("부재 상세 조회: absenceId={}", absenceId);

            AbsenceResponseDto absence = absenceService.getAbsenceById(absenceId);
            if (absence == null) {
                log.warn("부재를 찾을 수 없음: absenceId={}", absenceId);
                return buildErrorResponse(HttpStatus.NOT_FOUND, "해당 부재 내역을 찾을 수 없습니다.");
            }

            log.info("부재 상세 조회 완료: absenceId={}", absenceId);
            return buildSuccessResponse(absence, "상세 조회 성공");
        } catch (Exception e) {
            log.error("상세 조회 중 오류 발생", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "상세 조회 중 오류 발생: " + e.getMessage());
        }
    }

    // 부재 수정
    @PutMapping("/{absenceId}")
    public ResponseEntity<CommonResDto<AbsenceResponseDto>> updateAbsence(
            @PathVariable Long absenceId,
            @RequestBody AbsenceUpdateRequestDto request,
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        if (userInfo == null) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다.");
        }
        try {
            log.info("부재 수정 요청: absenceId={}, userId={}", absenceId, userInfo.getEmployeeNo());

            AbsenceResponseDto updatedAbsence = absenceService.updateAbsence(absenceId, request, String.valueOf(userInfo.getEmployeeNo()));

            log.info("부재 수정 완료: absenceId={}", absenceId);
            return buildSuccessResponse(updatedAbsence, "부재 정보가 성공적으로 업데이트되었습니다.");
        } catch (IllegalArgumentException e) {
            log.warn("부재 수정 실패 - 잘못된 요청: {}", e.getMessage());
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("부재 정보 업데이트 중 오류 발생", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "부재 정보 업데이트 중 오류 발생: " + e.getMessage());
        }
    }

    // 부재 삭제
    @DeleteMapping("/{absenceId}")
    public ResponseEntity<CommonResDto<Void>> deleteAbsence(
            @PathVariable Long absenceId,
            @AuthenticationPrincipal TokenUserInfo userInfo) {
        if (userInfo == null) {
            return buildErrorResponse(HttpStatus.UNAUTHORIZED, "인증 정보가 없습니다.");
        }
        try {
            log.info("부재 삭제 요청: absenceId={}, userId={}", absenceId, userInfo.getEmployeeNo());

            absenceService.deleteAbsence(absenceId, String.valueOf(userInfo.getEmployeeNo()));

            log.info("부재 삭제 완료: absenceId={}", absenceId);
            return buildSuccessResponse(null, "부재 정보가 성공적으로 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            log.warn("부재 삭제 실패 - 잘못된 요청: {}", e.getMessage());
            return buildErrorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            log.error("부재 정보 삭제 중 오류 발생", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "부재 정보 삭제 중 오류 발생: " + e.getMessage());
        }
    }

    // 부재 승인 (HR용)
    @PostMapping("/{absenceId}/approve")
    public ResponseEntity<CommonResDto<Void>> approveAbsence(
            @PathVariable Long absenceId,
            @RequestParam String approverId) {
        try {
            log.info("부재 승인 요청: absenceId={}, approverId={}", absenceId, approverId);

            absenceService.approveAbsence(absenceId, approverId);

            log.info("부재 승인 완료: absenceId={}", absenceId);
            return buildSuccessResponse(null, "부재가 성공적으로 승인되었습니다.");
        } catch (IllegalArgumentException e) {
            log.warn("부재 승인 실패 - 잘못된 요청: {}", e.getMessage());
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("부재 승인 중 오류 발생", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "부재 승인 중 오류 발생: " + e.getMessage());
        }
    }

    // 부재 반려 (HR용)
    @PostMapping("/{absenceId}/reject")
    public ResponseEntity<CommonResDto<Void>> rejectAbsence(
            @PathVariable Long absenceId,
            @RequestParam String approverId,
            @RequestParam String rejectComment) {
        try {
            log.info("부재 반려 요청: absenceId={}, approverId={}", absenceId, approverId);

            absenceService.rejectAbsence(absenceId, approverId, rejectComment);

            log.info("부재 반려 완료: absenceId={}", absenceId);
            return buildSuccessResponse(null, "부재가 성공적으로 반려되었습니다.");
        } catch (IllegalArgumentException e) {
            log.warn("부재 반려 실패 - 잘못된 요청: {}", e.getMessage());
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("부재 반려 중 오류 발생", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "부재 반려 중 오류 발생: " + e.getMessage());
        }
    }

    // 결재용 부재 목록 조회 (대기 중)
    @GetMapping("/approval")
    public ResponseEntity<CommonResDto<Page<AbsenceResponseDto>>> getAbsencesForApproval(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("결재용 부재 목록 조회: page={}, size={}", page, size);

            Pageable pageable = PageRequest.of(page, size);
            Page<AbsenceResponseDto> absences = absenceService.getAbsencesForApproval(pageable);

            log.info("결재용 부재 목록 조회 완료: totalElements={}", absences.getTotalElements());
            return buildSuccessResponse(absences, "결재용 부재 목록 조회 성공");
        } catch (Exception e) {
            log.error("결재용 부재 목록 조회 중 오류 발생", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "결재용 부재 목록 조회 중 오류 발생: " + e.getMessage());
        }
    }

    // 처리된 부재 목록 조회
    @GetMapping("/processed")
    public ResponseEntity<CommonResDto<Page<AbsenceResponseDto>>> getProcessedAbsences(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("처리된 부재 목록 조회: page={}, size={}", page, size);

            Pageable pageable = PageRequest.of(page, size);
            Page<AbsenceResponseDto> absences = absenceService.getProcessedAbsences(pageable);

            log.info("처리된 부재 목록 조회 완료: totalElements={}", absences.getTotalElements());
            return buildSuccessResponse(absences, "처리된 부재 목록 조회 성공");
        } catch (Exception e) {
            log.error("처리된 부재 목록 조회 중 오류 발생", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "처리된 부재 목록 조회 중 오류 발생: " + e.getMessage());
        }
    }

    // 결재 상태별 부재 목록 조회
    @GetMapping("/status/{approvalStatus}")
    public ResponseEntity<CommonResDto<List<AbsenceResponseDto>>> getAbsencesByStatus(
            @PathVariable ApprovalStatus approvalStatus) {
        try {
            log.info("결재 상태별 부재 목록 조회: status={}", approvalStatus);

            List<AbsenceResponseDto> absences = absenceService.getAbsencesByStatus(approvalStatus);

            log.info("결재 상태별 부재 목록 조회 완료: count={}", absences.size());
            return buildSuccessResponse(absences, "결재 상태별 부재 목록 조회 성공");
        } catch (Exception e) {
            log.error("결재 상태별 부재 목록 조회 중 오류 발생", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "결재 상태별 부재 목록 조회 중 오류 발생: " + e.getMessage());
        }
    }

    // 결재자별 처리한 부재 목록 조회
    @GetMapping("/approver/{approverId}")
    public ResponseEntity<CommonResDto<List<AbsenceResponseDto>>> getAbsencesByApprover(
            @PathVariable String approverId) {
        try {
            log.info("결재자별 부재 목록 조회: approverId={}", approverId);

            List<AbsenceResponseDto> absences = absenceService.getAbsencesByApprover(approverId);

            log.info("결재자별 부재 목록 조회 완료: count={}", absences.size());
            return buildSuccessResponse(absences, "결재자별 부재 목록 조회 성공");
        } catch (Exception e) {
            log.error("결재자별 부재 목록 조회 중 오류 발생", e);
            return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "결재자별 부재 목록 조회 중 오류 발생: " + e.getMessage());
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