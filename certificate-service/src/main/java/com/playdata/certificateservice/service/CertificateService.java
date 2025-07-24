package com.playdata.certificateservice.service;

import com.playdata.certificateservice.dto.CertificateReqDto;
import com.playdata.certificateservice.dto.CertificateResDto;
import com.playdata.certificateservice.dto.UserFeignResDto;
import com.playdata.certificateservice.entity.Certificate;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;

public interface CertificateService {
    // 증명서 신청
    @Transactional
    void createCertificate(CertificateReqDto dto);

    byte[] generateCertificatePdf(Long id);

    byte[] generatePdf(Certificate certificate, UserFeignResDto userInfo, InputStream fontStream) throws IOException;

    // 텍스트를 셀 중앙에 그리는 함수
    void drawCellText(PDPageContentStream cs, String text, PDFont font, int fontSize,
                      float x, float y, float width, float height) throws IOException;

    // null safe
    String safe(String value);

    // 증명서 조회
    Page<CertificateResDto> listCertificates(Long employeeNo, Pageable pageable);

    // 증명서 수정
    void updateCertificate(Long id, CertificateReqDto dto);

    // 증명서 삭제
    void deleteCertificate(Long id);
}
