package com.playdata.certificateservice.controller;

import com.playdata.certificateservice.common.dto.CommonResDto;
import com.playdata.certificateservice.dto.CertificateReqDto;
import com.playdata.certificateservice.service.CertificateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/certificate")
@RequiredArgsConstructor
@Slf4j
@RefreshScope
public class CertificateController {

    private final CertificateService certificateService;

    @PostMapping("/application")
    public ResponseEntity<?> createCertificate(@RequestBody CertificateReqDto dto) {
        certificateService.createCertificate(dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/list/{employeeNo}")
    public ResponseEntity<?> listCertificates(@PathVariable Long employeeNo ,@PageableDefault(sort = "certificateId") Pageable pageable) {
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK,
                "Success", certificateService.listCertificates(employeeNo, pageable)),
                HttpStatus.OK);
    }

    @PutMapping("/certificate/{id}")
    public ResponseEntity<?> updateCertificate(@PathVariable("id") Long id, @RequestBody CertificateReqDto dto) {
        certificateService.updateCertificate(id, dto);
        CommonResDto resDto = new CommonResDto(HttpStatus.OK, "수정 완료되었습니다.", null);
        return ResponseEntity.ok().body(resDto);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCertificate(@PathVariable("id") Long id) {
        certificateService.deleteCertificate(id);
        CommonResDto resDto = new CommonResDto(HttpStatus.OK, "삭제 완료되었습니다.", id);
        return ResponseEntity.ok().body(resDto);
    }

    @GetMapping("/print/{id}")
    public ResponseEntity<byte[]> printCertificatePdf(@PathVariable Long id) {
        byte[] pdfBytes = certificateService.generateCertificatePdf(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=certificate_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }

}
