package com.playdata.certificateservice.service;


import com.playdata.certificateservice.client.HrServiceClient;
import com.playdata.certificateservice.common.dto.CommonResDto;
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
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final HrServiceClient hrServiceClient;

    // 증명서 신청
    @Transactional
    @Override
    public void createCertificate(CertificateReqDto dto) {
        // 1. Certificate 객체 생성 및 저장
        Certificate certificate = Certificate.builder()
                .certificateId(dto.getCertificateId())
                .employeeNo(dto.getEmployeeNo())
                .type(Type.valueOf(dto.getType().name()))
                .status(Status.REQUESTED) // 기본값
                .purpose(dto.getPurpose())
                .requestDate(dto.getRequestDate())
                .approveDate(dto.getApproveDate())
                .build();

        log.info("Saved certificate: {}", certificate);

        certificateRepository.save(certificate);
    }

    @Override
    public byte[] generateCertificatePdf(Long id) {
        Certificate certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("증명서 없음"));

        CommonResDto<UserFeignResDto> response = hrServiceClient.getUserById(certificate.getEmployeeNo());
        UserFeignResDto userInfo = response.getResult();
        log.info("generateCertificatePdf - userInfo: {}", userInfo); // 주민등록번호 포함 확인
        if (userInfo == null) throw new EntityNotFoundException("유저 정보 없음");

        try {
            String fontPath = "/Users/yeni/Downloads/nanum-gothic/NanumGothic.ttf";
            return generatePdf(certificate, userInfo, fontPath);
        } catch (IOException e) {
            throw new RuntimeException("PDF 생성 실패", e);
        }
    }

    @Override
    public byte[] generatePdf(Certificate certificate, UserFeignResDto userInfo, String fontPath) throws IOException {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDFont font = PDType0Font.load(document, new FileInputStream(fontPath));

            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();

            // 1. 표 위치 및 크기(A4 기준) - 너비 더 넓게
            float tableX = 40;
            float tableY = pageHeight - 180; // 위쪽에 배치
            float tableWidth = pageWidth - 80; // (기존 120 → 80)
            float rowHeight = 44;

            // 2. 행 Y좌표 (5줄)
            float[] rowYs = new float[5];
            for (int i = 0; i < 5; i++) rowYs[i] = tableY - rowHeight * i;

            // 3. 열 너비 - 3,4번째(주민등록번호/직위)가 더 넓음: 15%/25%/30%/30%
            float[] colWidths = {
                    tableWidth * 0.17f,  // 성명/주소/소속 (살짝 넓게)
                    tableWidth * 0.27f,  // 이름/주소/부서 (살짝 넓게)
                    tableWidth * 0.28f,  // 주민등록번호/직위 (살짝 줄임)
                    tableWidth * 0.28f   // 주민등록번호/직위 값 (살짝 줄임)
            };
            float[] colXs = new float[colWidths.length + 1];
            colXs[0] = tableX;
            for (int i = 1; i <= colWidths.length; i++) {
                colXs[i] = colXs[i - 1] + colWidths[i - 1];
            }

            // 데이터 추출
            String userName = safe(userInfo.getUserName());
            String rrn = safe(userInfo.getResidentRegNo());
            String address = safe(userInfo.getAddress());
            String dept = safe(userInfo.getDepartmentName());
            String position = safe(userInfo.getPositionName());
            String period = userInfo.getHireDate() != null ? userInfo.getHireDate() + " ~ 현재" : "";
            String purpose = safe(certificate.getPurpose());

            String title = "재직증명서";
            if (certificate.getType() != null) {
                title = switch (certificate.getType().name()) {
                    case "EMPLOYMENT" -> "재직증명서";
                    case "CAREER" -> "경력증명서";
                    default -> certificate.getType().name();
                };
            }

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                // 1. 타이틀
                cs.beginText();
                cs.setFont(font, 36);
                float titleWidth = font.getStringWidth(title) / 1000 * 36;
                cs.newLineAtOffset((pageWidth - titleWidth) / 2, tableY + 60);
                cs.showText(title);
                cs.endText();

                // 2. 표 외곽선(4행)
                cs.setLineWidth(1.3f);
                cs.addRect(tableX, tableY - rowHeight * 4, tableWidth, rowHeight * 4);
                cs.stroke();

                // 3. 가로선
                for (int i = 1; i < 4; i++) {
                    cs.moveTo(tableX, tableY - rowHeight * i);
                    cs.lineTo(tableX + tableWidth, tableY - rowHeight * i);
                    cs.stroke();
                }

                // 4. 세로선 (1,3행만 네 칸)
                for (int i = 1; i < colXs.length - 1; i++) {
                    cs.moveTo(colXs[i], rowYs[0]);
                    cs.lineTo(colXs[i], rowYs[1]);
                    cs.stroke();

                    cs.moveTo(colXs[i], rowYs[2]);
                    cs.lineTo(colXs[i], rowYs[3]);
                    cs.stroke();
                }
                // 2행(주소), 4행(기간): 첫 칸만 나누기
                cs.moveTo(colXs[1], rowYs[1]);
                cs.lineTo(colXs[1], rowYs[2]);
                cs.stroke();

                cs.moveTo(colXs[1], rowYs[3]);
                cs.lineTo(colXs[1], rowYs[4]);
                cs.stroke();

                // 5. 표 텍스트 (폰트 20)
                cs.setFont(font, 20);
                drawCellText(cs, "성    명", font, 18, colXs[0], rowYs[0], colWidths[0], rowHeight);
                drawCellText(cs, userName, font, 15, colXs[1], rowYs[0], colWidths[1], rowHeight);
                drawCellText(cs, "주민등록번호", font, 18, colXs[2], rowYs[0], colWidths[2], rowHeight);
                drawCellText(cs, rrn, font, 15, colXs[3], rowYs[0], colWidths[3], rowHeight);

                drawCellText(cs, "주    소", font, 18, colXs[0], rowYs[1], colWidths[0], rowHeight);
                drawCellText(cs, address, font, 15, colXs[1], rowYs[1], colWidths[1] + colWidths[2] + colWidths[3], rowHeight);

                drawCellText(cs, "소    속", font, 18, colXs[0], rowYs[2], colWidths[0], rowHeight);
                drawCellText(cs, dept, font, 15, colXs[1], rowYs[2], colWidths[1], rowHeight);
                drawCellText(cs, "직    위", font, 18, colXs[2], rowYs[2], colWidths[2], rowHeight);
                drawCellText(cs, position, font, 15, colXs[3], rowYs[2], colWidths[3], rowHeight);

                drawCellText(cs, "기    간", font, 18, colXs[0], rowYs[3], colWidths[0], rowHeight);
                drawCellText(cs, period, font, 15, colXs[1], rowYs[3], colWidths[1] + colWidths[2] + colWidths[3], rowHeight);

                // 6. 표 아래 문구 (type별로 다르게)
                String footerText;
                if (certificate.getType() != null) {
                    footerText = switch (certificate.getType().name()) {
                        case "EMPLOYMENT" -> "상기와 같이 재직 중임을 증명함";
                        case "CAREER" -> "상기와 같이 근무하였음을 증명함";
                        default -> "상기와 같이 사실임을 증명함";
                    };
                } else {
                    footerText = "상기와 같이 사실임을 증명함";
                }
                cs.beginText();
                cs.setFont(font, 17);
                cs.newLineAtOffset(pageWidth / 2 - (font.getStringWidth(footerText) / 1000 * 17) / 2, rowYs[4] - 48);
                cs.showText(footerText);
                cs.endText();

                // 7. 용도
                cs.beginText();
                cs.setFont(font, 16);
                cs.newLineAtOffset(tableX, rowYs[4] - 90);
                cs.showText("용도 : " + (purpose.isEmpty() ? "제출용" : purpose));
                cs.endText();

                // 8. 하단 회사 정보 (중앙, 간격 크게)
                float bottomY = 100; // 기존 120 → 100로 더 아래로 내림
                String corpAddress = "서울시 강남구 ○○로 123";
                String corpName = "○○컴퍼니";
                String ceo = "박대표";

                // 주소 (회사명 위, 간격 +80)
                cs.beginText();
                cs.setFont(font, 15);
                float addrWidth = font.getStringWidth("주소 : " + corpAddress) / 1000 * 14;
                cs.newLineAtOffset((pageWidth - addrWidth) / 2, bottomY + 80);
                cs.showText("주소 : " + corpAddress);
                cs.endText();

                // 회사명 (중앙, 간격 +45)
                cs.beginText();
                cs.setFont(font, 16);
                float corpWidth = font.getStringWidth("회사명 : " + corpName) / 1000 * 16;
                cs.newLineAtOffset((pageWidth - corpWidth) / 2, bottomY + 45);
                cs.showText("회사명 : " + corpName);
                cs.endText();

                // 대표이사 (간격 +10)
                cs.beginText();
                cs.setFont(font, 16);
                float ceoWidth = font.getStringWidth("대표이사 : " + ceo) / 1000 * 16;
                cs.newLineAtOffset((pageWidth - ceoWidth) / 2, bottomY + 10);
                cs.showText("대표이사 : " + ceo);
                cs.endText();

                // (인) 텍스트
                float inTextX = pageWidth - 90;
                float inTextY = bottomY + 10;
                cs.beginText();
                cs.setFont(font, 15);
                cs.newLineAtOffset(inTextX, inTextY);
                cs.showText("(인)");
                cs.endText();

                // 도장 이미지
                ClassPathResource resource = new ClassPathResource("9a94a2dad35c7ace.png");
                byte[] sealBytes = resource.getInputStream().readAllBytes();
                PDImageXObject sealImage = PDImageXObject.createFromByteArray(document, sealBytes, "seal");

                // (인) 텍스트 중앙에 도장 중앙이 오도록, 크기 65x65
                float sealWidth = 85;
                float sealHeight = 85;
                float inTextFontSize = 14;
                float inTextWidth = font.getStringWidth("(인)") / 1000 * inTextFontSize;
                float inTextHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * inTextFontSize;
                float sealX = inTextX + inTextWidth/2 - sealWidth/2;
                float sealY = inTextY + inTextHeight/2 - sealHeight/2;

                cs.drawImage(sealImage, sealX, sealY, sealWidth, sealHeight);
            }

            document.save(baos);
            return baos.toByteArray();
        }
    }

    // 텍스트를 셀 중앙에 그리는 함수
    @Override
    public void drawCellText(PDPageContentStream cs, String text, PDFont font, int fontSize,
                             float x, float y, float width, float height) throws IOException {
        log.info("drawCellText: {}", text); // 실제 PDF에 들어갈 값
        if (text == null) text = "";
        float textWidth = font.getStringWidth(text) / 1000 * fontSize;
        float textHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * fontSize;
        float textX = x + (width - textWidth) / 2;
        float textY = y - height / 2 - textHeight / 4; // 살짝 내려줘야 자연스럽다
        cs.beginText();
        cs.setFont(font, fontSize);
        cs.newLineAtOffset(textX, textY);
        cs.showText(text);
        cs.endText();
    }

    // null safe
    @Override
    public String safe(String value) {
        return value == null ? "" : value;
    }

    // 증명서 조회
    @Override
    public Page<CertificateResDto> listCertificates(Long employeeNo, Pageable pageable) {
        Page<Certificate> certificates = certificateRepository.findByEmployeeNo(employeeNo, pageable);
        return certificates.map(certificate -> CertificateResDto.builder()
                .certificateId(certificate.getCertificateId())
                .type(Type.valueOf(certificate.getType().name()))
                .requestDate(certificate.getRequestDate())
                .approveDate(certificate.getApproveDate())
                .status(Status.valueOf(certificate.getStatus().name()))
                .purpose(certificate.getPurpose())
                .build());
    }

    // 증명서 수정
    @Override
    public void updateCertificate(Long id, CertificateReqDto dto) {
        Certificate certificate = certificateRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("certificate not found")
        );

        certificate.setRequestDate(dto.getRequestDate());
        certificate.setType(Type.valueOf(dto.getType().name()));
        certificate.setPurpose(dto.getPurpose());

        certificateRepository.save(certificate);
    }

    // 증명서 삭제
    @Override
    public void deleteCertificate(Long id) {
        Certificate certificate = certificateRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("certificate with id " + id + " not found")
        );
        certificateRepository.delete(certificate);
    }


}