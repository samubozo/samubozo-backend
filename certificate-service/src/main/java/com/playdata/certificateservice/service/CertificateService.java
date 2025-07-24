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

    @Transactional
    void createCertificate(CertificateReqDto dto);

    byte[] generateCertificatePdf(Long id);

    byte[] generatePdf(Certificate certificate, UserFeignResDto userInfo, InputStream fontStream) throws IOException;

    void drawCellText(PDPageContentStream cs, String text, PDFont font, int fontSize,
                      float x, float y, float width, float height) throws IOException;

    String safe(String value);

    Page<CertificateResDto> listCertificates(Long employeeNo, Pageable pageable);

    void updateCertificate(Long id, CertificateReqDto dto);

    void deleteCertificate(Long id);

}
