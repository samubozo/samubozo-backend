package com.playdata.certificateservice.controller;

import com.playdata.certificateservice.client.hr.HrServiceClient;
import com.playdata.certificateservice.client.hr.dto.UserFeignResDto;
import com.playdata.certificateservice.common.auth.TokenUserInfo;
import com.playdata.certificateservice.common.dto.CommonResDto;
import com.playdata.certificateservice.dto.CertificateRejectRequestDto;
import com.playdata.certificateservice.dto.CertificateReqDto;
import com.playdata.certificateservice.dto.CertificateResDto;
import com.playdata.certificateservice.entity.Certificate;
import com.playdata.certificateservice.entity.Status;
import com.playdata.certificateservice.service.CertificateService;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/certificate")
@RequiredArgsConstructor
@Slf4j
@RefreshScope
public class CertificateController {

    private final CertificateService certificateService;
    private final HrServiceClient hrServiceClient;

    // 증명서 발급 (사용자)
    @PostMapping("/application")
    public ResponseEntity<?> createCertificate(@AuthenticationPrincipal TokenUserInfo userInfo, @RequestBody CertificateReqDto dto) {
        log.info("Create certificate request by user {}: {}", userInfo.getEmployeeNo(), dto);
        certificateService.createCertificate(userInfo, dto);
        return ResponseEntity.ok().build();
    }

    // 내 증명서 조회 (사용자)
    @GetMapping("/my-list")
    public ResponseEntity<?> listMyCertificates(@AuthenticationPrincipal TokenUserInfo userInfo, @PageableDefault(sort = "certificateId") Pageable pageable) {
        log.info("List certificates request by user {}: {}", userInfo.getEmployeeNo(), pageable);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK,
                "Success", certificateService.listMyCertificates(userInfo, pageable)), HttpStatus.OK);
    }

    // 내 증명서 수정 (사용자)
    @PutMapping("/my-certificate/{id}")
    public ResponseEntity<?> updateMyCertificate(@AuthenticationPrincipal TokenUserInfo userInfo, @PathVariable("id") Long id, @RequestBody CertificateReqDto dto) {
        log.info("Update certificate request by user {}: id={}, dto={}", userInfo.getEmployeeNo(), id, dto);
        certificateService.updateMyCertificate(userInfo, id, dto);
        CommonResDto resDto = new CommonResDto(HttpStatus.OK, "수정 완료되었습니다.", null);
        return ResponseEntity.ok().body(resDto);
    }

    // 내 증명서 삭제 (사용자 - 신청 취소)
    @DeleteMapping("/my-delete/{id}")
    public ResponseEntity<?> deleteMyCertificate(@AuthenticationPrincipal TokenUserInfo userInfo, @PathVariable("id") Long id) {
        log.info("Delete certificate request by user {}: id={}", userInfo.getEmployeeNo(), id);
        certificateService.deleteMyCertificate(userInfo, id);
        CommonResDto resDto = new CommonResDto(HttpStatus.OK, "삭제 완료되었습니다.", id);
        return ResponseEntity.ok().body(resDto);
    }

    // 내 증명서 인쇄 (사용자 + HR)
    @GetMapping("/my-print/{id}")
    public ResponseEntity<byte[]> printMyCertificatePdf(@AuthenticationPrincipal TokenUserInfo userInfo, @PathVariable Long id) {
        log.info("=== 증명서 인쇄 컨트롤러 호출 ===");
        log.info("사용자: {}, 증명서 ID: {}", userInfo.getEmployeeNo(), id);
        log.info("HR 권한: {}", userInfo.getHrRole());

        try {
            Certificate certificate = certificateService.getCertificateById(id);
            log.info("증명서 조회 성공: {}", certificate);

            // 1. 소유권 확인 또는 HR 권한 확인
            boolean isOwner = certificate.getEmployeeNo().equals(userInfo.getEmployeeNo());
            boolean isHR = "Y".equals(userInfo.getHrRole());

            log.info("권한 확인: 소유자={}, HR={}", isOwner, isHR);

            if (!isOwner && !isHR) {
                log.error("권한 없음: 사용자={}, 증명서 소유자={}", userInfo.getEmployeeNo(), certificate.getEmployeeNo());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // 2. 승인된 증명서만 출력 가능
            log.info("증명서 상태: {}", certificate.getStatus());
            if (certificate.getStatus() != Status.APPROVED) {
                log.error("승인되지 않은 증명서: 상태={}", certificate.getStatus());
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("승인되지 않은 증명서는 출력할 수 없습니다.".getBytes());
            }

            log.info("PDF 생성 시작");
            byte[] pdfBytes = certificateService.generateMyCertificatePdf(userInfo, id);
            log.info("PDF 생성 완료: 크기={} bytes", pdfBytes.length);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=certificate_" + id + ".pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(pdfBytes.length)
                    .body(pdfBytes);

        } catch (Exception e) {
            log.error("증명서 인쇄 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    // [내부호출] 증명서 승인 처리 (approval-service 전용)
    @PutMapping("/internal/certificates/{id}/approve")
    public ResponseEntity<Void> approveCertificateInternal(@PathVariable("id") Long id, @RequestParam("approverId") Long approverId, @RequestParam("approverName") String approverName) {
        log.info("Internal approve certificate request for id={}, approverId={}, approverName={}", id, approverId, approverName);
        certificateService.approveCertificateInternal(id, approverId, approverName);
        return ResponseEntity.ok().build();
    }

    // [내부호출] 증명서 반려 처리 (approval-service 전용)
    @PutMapping("/internal/certificates/{id}/reject")
    public ResponseEntity<Void> rejectCertificateInternal(@PathVariable("id") Long id, @RequestParam("approverName") String approverName) {
        log.info("Internal reject certificate request for id={}, approverName={}", id, approverName);
        // rejectComment는 내부 호출에서 필요하지 않으므로, 서비스 계층에서 처리
        certificateService.rejectCertificateInternal(id, null, approverName); // rejectComment는 내부 호출에서 필요하지 않으므로 null 전달
        return ResponseEntity.ok().build();
    }

    // 모든 증명서 조회 (HR 전용)
    @GetMapping("/list/all")
    public ResponseEntity<?> listAllCertificates(@AuthenticationPrincipal TokenUserInfo userInfo, @RequestParam(required = false) Long employeeNo, @PageableDefault(sort = "certificateId") Pageable pageable) {
        log.info("List all certificates request by HR user {}: employeeNo={}, pageable={}", userInfo.getEmployeeNo(), employeeNo, pageable);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK,
                "Success", certificateService.listAllCertificates(userInfo, employeeNo, pageable)), HttpStatus.OK);
    }

    // 증명서 승인 (HR 전용)
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveCertificate(@AuthenticationPrincipal TokenUserInfo userInfo, @PathVariable("id") Long id) {
        log.info("Approve certificate request by HR user {}: id={}", userInfo.getEmployeeNo(), id);
        certificateService.approveCertificate(id, userInfo);
        CommonResDto resDto = new CommonResDto(HttpStatus.OK, "증명서가 승인되었습니다.", null);
        return ResponseEntity.ok().body(resDto);
    }

    // 증명서 반려 (HR 전용)
    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectCertificate(@AuthenticationPrincipal TokenUserInfo userInfo, @PathVariable("id") Long id, @RequestBody CertificateRejectRequestDto rejectDto) {
        log.info("Reject certificate request by HR user {}: id={}, rejectComment={}", userInfo.getEmployeeNo(), id, rejectDto.getRejectComment());
        certificateService.rejectCertificate(id, userInfo, rejectDto);
        CommonResDto resDto = new CommonResDto(HttpStatus.OK, "증명서가 반려되었습니다.", null);
        return ResponseEntity.ok().body(resDto);
    }

    // 증명서 단건 상세 조회
    @GetMapping("/{id}")
    public ResponseEntity<?> getCertificateById(@PathVariable("id") Long id) {
        log.info("Get certificate by id: {}", id);
        Certificate certificate = certificateService.getCertificateById(id);

        // HR 서비스에서 신청자 및 결재자 정보 조회
        String applicantName = null;
        String departmentName = null;

        try {
            // 신청자 정보 조회
            CommonResDto<UserFeignResDto> hrResponse = hrServiceClient.getUserById(certificate.getEmployeeNo());
            if (hrResponse != null && hrResponse.getResult() != null) {
                UserFeignResDto applicantInfo = hrResponse.getResult();
                applicantName = applicantInfo.getUserName();
                if (applicantInfo.getDepartment() != null) {
                    departmentName = applicantInfo.getDepartment().getName();
                }
            }
        } catch (FeignException e) {
            log.error("HR 서비스 통신 오류: {}", e.getMessage());
            // 통신 오류 발생 시 이름 정보 없이 진행
        }

        CertificateResDto resDto = CertificateResDto.builder()
                .certificateId(certificate.getCertificateId())
                .employeeNo(certificate.getEmployeeNo())
                .type(certificate.getType())
                .requestDate(certificate.getRequestDate())
                .approveDate(certificate.getApproveDate())
                .processedAt(certificate.getProcessedAt())
                .status(certificate.getStatus())
                .purpose(certificate.getPurpose())
                .applicantName(applicantName)
                .departmentName(departmentName)
                .approverName(certificate.getApproverName())
                .rejectComment(certificate.getRejectComment())
                .build();

        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK, "Success", resDto), HttpStatus.OK);
    }
}
