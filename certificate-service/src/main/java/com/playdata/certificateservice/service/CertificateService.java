package com.playdata.certificateservice.service;


import com.playdata.certificateservice.client.HrServiceClient;
import com.playdata.certificateservice.dto.CertificateReqDto;
import com.playdata.certificateservice.dto.CertificateResDto;
import com.playdata.certificateservice.dto.UserFeignResDto;
import com.playdata.certificateservice.entity.Certificate;
import com.playdata.certificateservice.entity.Status;
import com.playdata.certificateservice.entity.Type;
import com.playdata.certificateservice.repository.CertificateRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateService {

    private final CertificateRepository certificateRepository;
    private final HrServiceClient hrServiceClient;

    // 증명서 신청
    @Transactional
    public void createCertificate(CertificateReqDto dto) {
        // 1. Certificate 객체 생성 및 저장
        Certificate certificate = Certificate.builder()
                .employeeNo(dto.getEmployeeNo())
                .type(Type.valueOf(dto.getType().name()))
                .status(Status.REQUESTED) // 기본값
                .purpose(dto.getPurpose())
                .requestDate(dto.getRequestDate())
                .build();

        certificateRepository.save(certificate);

        // 2. FeignClient로 유저 정보 조회
        UserFeignResDto userInfo = hrServiceClient.getUserById(dto.getEmployeeNo());
        if (userInfo == null) {
            throw new EntityNotFoundException("유저 정보 없음");
        }

        // 3. PDF 생성 및 저장
        try {
            // 필요에 따라 fontPath, savePath 세팅
            String fontPath = "/경로/폰트.ttf";
            String savePath = "/경로/pdf";
            generatePdf(certificate, userInfo, fontPath, savePath);
        } catch (IOException e) {
            throw new RuntimeException("PDF 생성 실패", e);
        }
    }

    public void generatePdf(Certificate certificate, UserFeignResDto userInfo, String fontPath, String savePath) throws IOException {
        PDDocument document = new PDDocument();
        PDPage page = new PDPage();
        document.addPage(page);

        PDFont font = PDType0Font.load(document, new FileInputStream(fontPath));

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            contentStream.beginText();
            contentStream.setFont(font, 14);
            contentStream.newLineAtOffset(100, 700);
            contentStream.showText("유저: " + userInfo.getUserName()); // 실제 필드명에 맞게
            contentStream.newLineAtOffset(0, -30);
            contentStream.showText("타입: " + certificate.getType());
            contentStream.newLineAtOffset(0, -30);
            contentStream.showText("신청일: " + certificate.getRequestDate());
            contentStream.newLineAtOffset(0, -30);
            contentStream.showText("용도: " + certificate.getPurpose());
            contentStream.endText();
        }

        document.save(savePath + "/certificate_" + certificate.getCertificateId() + ".pdf");
        document.close();
    }

    // 증명서 조회
    public Page<CertificateResDto> listCertificates(Pageable pageable) {
        Page<Certificate> certificates = certificateRepository.findAll(pageable);
        return certificates.map(certificate -> CertificateResDto.builder()
                .certificateId(certificate.getCertificateId())
                .type(Type.valueOf(certificate.getType().name()))
                .requestDate(certificate.getRequestDate())
                .approveDate(certificate.getApproveDate())
                .status(Status.valueOf(certificate.getStatus().name()))
                .purpose(certificate.getPurpose())
                .build());
    }

}