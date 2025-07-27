package com.playdata.certificateservice.service;

import com.playdata.certificateservice.common.auth.TokenUserInfo;
import com.playdata.certificateservice.dto.CertificateRejectRequestDto; // 추가
import com.playdata.certificateservice.dto.CertificateReqDto;
import com.playdata.certificateservice.dto.CertificateResDto;
import com.playdata.certificateservice.client.hr.dto.UserFeignResDto;
import com.playdata.certificateservice.entity.Certificate;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

public interface CertificateService {
    // 증명서 신청
    @Transactional
    void createCertificate(TokenUserInfo userInfo, CertificateReqDto dto);

    // 내 증명서 조회
    Page<CertificateResDto> listMyCertificates(TokenUserInfo userInfo, Pageable pageable);

    // 내 증명서 수정
    void updateMyCertificate(TokenUserInfo userInfo, Long id, CertificateReqDto dto);

    // 내 증명서 삭제 (신청 취소)
    void deleteMyCertificate(TokenUserInfo userInfo, Long id);

    // 내 증명서 인쇄
    byte[] generateMyCertificatePdf(TokenUserInfo userInfo, Long id);

    byte[] generatePdf(Certificate certificate, UserFeignResDto userInfo) throws IOException;

    // 텍스트를 셀 중앙에 그리는 함수
    void drawCellText(PDPageContentStream cs, String text, PDFont font, int fontSize,
                      float x, float y, float width, float height) throws IOException;

    // null safe
    String safe(String value);

    // [내부호출] 증명서 승인 처리
    void approveCertificateInternal(Long id, Long approverId, String approverName);

    // [내부호출] 증명서 반려 처리
    void rejectCertificateInternal(Long id, String rejectComment, String approverName); // 변경

    // 모든 증명서 조회 (HR 전용)
    Page<CertificateResDto> listAllCertificates(TokenUserInfo userInfo, Long employeeNo, Pageable pageable);

    // 증명서 승인 (HR 전용)
    void approveCertificate(Long id, TokenUserInfo userInfo);

    // 증명서 반려 (HR 전용)
    void rejectCertificate(Long id, TokenUserInfo userInfo, CertificateRejectRequestDto rejectDto); // 변경

    // 증명서 ID로 조회
    Certificate getCertificateById(Long id);
}
