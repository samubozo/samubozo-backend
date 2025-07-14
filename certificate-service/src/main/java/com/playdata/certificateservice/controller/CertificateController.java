package com.playdata.certificateservice.controller;

import com.playdata.certificateservice.common.dto.CommonResDto;
import com.playdata.certificateservice.dto.CertificateReqDto;
import com.playdata.certificateservice.service.CertificateService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/certificate")
@RequiredArgsConstructor
@Slf4j
@RefreshScope
public class CertificateController {

    private final CertificateService certificateService;

    // 증명서 발급
    @PostMapping("/application")
    public ResponseEntity<?> createCertificate(@RequestBody CertificateReqDto dto) {
        log.info("Create certificate request: {}", dto);
        certificateService.createCertificate(dto);
        return ResponseEntity.ok().build();
    }

    // 증명서 조회
    @GetMapping("/list")
    public ResponseEntity<?> listCertificates(@PageableDefault(size = 5, sort = "certificateId") Pageable pageable) {
        log.info("List certificates request: {}", pageable);
        return new ResponseEntity<>(new CommonResDto(HttpStatus.OK,
                "Success", certificateService.listCertificates(pageable)), HttpStatus.OK);
    }

    // 증명서 수정
    @PutMapping("/certificate/{id}")
    public ResponseEntity<?> updateCertificate(@PathVariable("id") Long id, CertificateReqDto dto) {
        certificateService.updateCertificate(id, dto);
        CommonResDto resDto = new CommonResDto(HttpStatus.OK, "수정 완료되었습니다.", null);
        return ResponseEntity.ok().body(resDto);
    }

    // 증명서 삭제
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCertificate(@PathVariable("id") Long id) {
        certificateService.deleteCertificate(id);
        CommonResDto resDto = new CommonResDto(HttpStatus.OK, "삭제 완료되었습니다.", id);
        return ResponseEntity.ok().body(resDto);
    }

}
