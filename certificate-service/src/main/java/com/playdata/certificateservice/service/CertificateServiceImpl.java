package com.playdata.certificateservice.service;


import com.playdata.certificateservice.client.ApprovalServiceClient;
import com.playdata.certificateservice.client.dto.ApprovalRequestResponseDto;
import com.playdata.certificateservice.client.dto.CertificateApprovalRequestCreateDto;
import com.playdata.certificateservice.client.hr.HrServiceClient;
import com.playdata.certificateservice.client.hr.dto.UserFeignResDto;
import com.playdata.certificateservice.client.hr.dto.UserResDto;
import com.playdata.certificateservice.common.auth.TokenUserInfo;
import com.playdata.certificateservice.common.dto.CommonResDto;
import com.playdata.certificateservice.dto.CertificateRejectRequestDto;
import com.playdata.certificateservice.dto.CertificateReqDto;
import com.playdata.certificateservice.dto.CertificateResDto;
import com.playdata.certificateservice.entity.Certificate;
import com.playdata.certificateservice.entity.Status;
import com.playdata.certificateservice.entity.Type;
import com.playdata.certificateservice.repository.CertificateRepository;
import feign.FeignException;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class CertificateServiceImpl implements CertificateService {

    private final CertificateRepository certificateRepository;
    private final HrServiceClient hrServiceClient;
    private final ApprovalServiceClient approvalServiceClient;

    // 증명서 신청 (사용자)
    @Transactional
    @Override
    public Certificate createCertificate(TokenUserInfo userInfo, CertificateReqDto dto) {
        // 1. Certificate 엔티티 생성 및 저장
        Certificate certificate = Certificate.builder()
                .employeeNo(userInfo.getEmployeeNo())
                .type(Type.valueOf(dto.getType().name()))
                .status(Status.PENDING)
                .purpose(dto.getPurpose())
                .requestDate(dto.getRequestDate())
                .build();
        Certificate savedCertificate = certificateRepository.save(certificate);
        log.info("1단계: 증명서 신청 내용 저장 성공. Certificate ID: {}", savedCertificate.getCertificateId());

        // 2. Approval-Service에 결재 요청을 보내기 위한 DTO 생성
        String title = savedCertificate.getType().name() + " 신청";
        CertificateApprovalRequestCreateDto approvalRequestDto = CertificateApprovalRequestCreateDto.builder()
                .certificateId(savedCertificate.getCertificateId())
                .title(title)
                .reason(savedCertificate.getPurpose()) // 증명서의 'purpose'를 결재 요청의 'reason'으로 사용
                .build();
        log.info("2단계: Approval-Service로 보낼 결재 요청 DTO 생성 완료. DTO: {}", approvalRequestDto);

        try {
            // 3. Approval-Service의 결재 요청 생성 API 호출
            log.info("3단계: Approval-Service API 호출 시작.");
            ApprovalRequestResponseDto approvalResponse = approvalServiceClient.createCertificateApprovalRequest(approvalRequestDto);
            log.info("4단계: Approval-Service API 호출 성공. Response: {}", approvalResponse);

            // 4. 결재 요청 ID를 Certificate 엔티티에 업데이트
            savedCertificate.setApprovalRequestId(approvalResponse.getId());
            certificateRepository.save(savedCertificate);
            log.info("5단계: Certificate에 ApprovalRequest ID 업데이트 성공. ApprovalRequest ID: {}", approvalResponse.getId());

        } catch (FeignException e) {
            log.error("Approval-Service API 호출 실패. Status: {}, Response: {}", e.status(), e.contentUTF8(), e);
            // 트랜잭션 롤백을 위해 런타임 예외 발생
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "결재 요청 생성에 실패했습니다.", e);
        }

        // 5. 최종적으로 결재 요청 정보까지 포함된 Certificate 객체 반환
        return savedCertificate;
    }

    // 내 증명서 조회 (사용자)
    @Override
    public Page<CertificateResDto> listMyCertificates(TokenUserInfo userInfo, Pageable pageable) {
        Page<Certificate> certificates = certificateRepository.findByEmployeeNo(userInfo.getEmployeeNo(), pageable);

        // HR 서비스에서 현재 로그인한 사용자의 상세 정보 조회
        String applicantName = null;
        String departmentName = null;
        try {
            CommonResDto<UserFeignResDto> hrResponse = hrServiceClient.getUserById(userInfo.getEmployeeNo());
            if (hrResponse != null && hrResponse.getResult() != null) {
                UserFeignResDto currentUserInfo = hrResponse.getResult();
                applicantName = currentUserInfo.getUserName();
                if (currentUserInfo.getDepartment() != null) {
                    departmentName = currentUserInfo.getDepartment().getName();
                }
            }
        } catch (FeignException e) {
            log.error("HR 서비스 통신 오류 (getUserById) for employeeNo {}: {}", userInfo.getEmployeeNo(), e.getMessage());
            // 오류 발생 시 이름/부서 정보 없이 진행
        }

        final String finalApplicantName = applicantName;
        final String finalDepartmentName = departmentName;

        return certificates.map(certificate -> {
            String approverName = null;
            if (certificate.getApprovalRequestId() != null) {
                try {
                    ApprovalRequestResponseDto approvalResponse = approvalServiceClient.getApprovalRequestById(certificate.getApprovalRequestId());
                    // [수정] 요청 타입이 'CERTIFICATE'인 경우에만 결재자 이름을 가져옵니다.
                    if (approvalResponse != null && "CERTIFICATE".equals(approvalResponse.getRequestType())) {
                        log.info("ApprovalRequestResponseDto from approval-service (listMyCertificates): approverId={}, approverName={}", approvalResponse.getApproverId(), approvalResponse.getApproverName());
                        approverName = approvalResponse.getApproverName();
                    } else {
                        log.warn("조회된 결재 요청(ID: {})이 증명서 타입이 아닙니다. (타입: {})",
                                certificate.getApprovalRequestId(), approvalResponse != null ? approvalResponse.getRequestType() : "null");
                    }
                } catch (FeignException e) {
                    // 404 Not Found는 다른 서비스에서 해당 ID를 찾지 못한 경우이므로 경고로 처리하고 계속 진행합니다.
                    if (e.status() == 404) {
                        log.warn("결재 서비스에서 ID {}에 해당하는 결재 요청을 찾을 수 없습니다.", certificate.getApprovalRequestId());
                    } else {
                        log.error("결재 서비스 통신 오류 (getApprovalRequestById) for approvalRequestId {}: {}", certificate.getApprovalRequestId(), e.getMessage());
                    }
                }
            }

            return CertificateResDto.builder()
                    .certificateId(certificate.getCertificateId())
                    .employeeNo(certificate.getEmployeeNo())
                    .type(Type.valueOf(certificate.getType().name()))
                    .requestDate(certificate.getRequestDate())
                    .approveDate(certificate.getApproveDate())
                    .processedAt(certificate.getProcessedAt())
                    .status(Status.valueOf(certificate.getStatus().name()))
                    .reason(certificate.getPurpose())
                    .applicantName(finalApplicantName)
                    .departmentName(finalDepartmentName)
                    .approverName(certificate.getApproverName())
                    .rejectComment(certificate.getRejectComment())
                    .build();
        });
    }

    // 내 증명서 수정 (사용자)
    @Override
    @Transactional
    public void updateMyCertificate(TokenUserInfo userInfo, Long id, CertificateReqDto dto) {
        Certificate certificate = certificateRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Certificate not found with id: " + id)
        );

        // 1. 소유권 확인
        if (!certificate.getEmployeeNo().equals(userInfo.getEmployeeNo())) {
            throw new IllegalStateException("본인의 증명서만 수정할 수 있습니다.");
        }

        // 2. 상태 확인: PENDING 상태일 때만 수정 가능
        if (certificate.getStatus() != Status.PENDING) {
            throw new IllegalStateException("결재 대기 중인 증명서만 수정할 수 있습니다. (현재 상태: " + certificate.getStatus() + ")");
        }

        certificate.setRequestDate(dto.getRequestDate());
        certificate.setType(Type.valueOf(dto.getType().name()));
        certificate.setPurpose(dto.getPurpose());

        certificateRepository.save(certificate);
        log.info("Certificate {} has been updated by user {}.", id, userInfo.getEmployeeNo());
    }

    // 내 증명서 삭제 (사용자 - 신청 취소)
    @Override
    @Transactional
    public void deleteMyCertificate(TokenUserInfo userInfo, Long id) {
        Certificate certificate = certificateRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Certificate not found with id: " + id)
        );

        // 1. 소유권 확인
        if (!certificate.getEmployeeNo().equals(userInfo.getEmployeeNo())) {
            throw new IllegalStateException("본인의 증명서만 삭제할 수 있습니다.");
        }

        // 2. 상태 확인: PENDING 상태일 때만 삭제 가능 (신청 취소)
        if (certificate.getStatus() != Status.PENDING) {
            throw new IllegalStateException("결재 대기 중인 증명서만 삭제할 수 있습니다. (현재 상태: " + certificate.getStatus() + ")");
        }

        certificateRepository.delete(certificate);
        log.info("Certificate {} has been deleted by user {}.", id, userInfo.getEmployeeNo());
    }

    // 내 증명서 인쇄 (사용자)
    @Override
    public byte[] generateMyCertificatePdf(TokenUserInfo userInfo, Long id) {
        Certificate certificate = certificateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("증명서 없음"));

        // 1. 소유권 확인
        if (!certificate.getEmployeeNo().equals(userInfo.getEmployeeNo())) {
            throw new IllegalStateException("본인의 증명서만 출력할 수 있습니다.");
        }

        // 2. 승인된 증명서만 출력 가능
        if (certificate.getStatus() != Status.APPROVED) {
            throw new IllegalStateException("승인되지 않은 증명서는 출력할 수 없습니다.");
        }

        CommonResDto<UserFeignResDto> response = hrServiceClient.getUserById(certificate.getEmployeeNo());
        UserFeignResDto userInfoRes = response.getResult();
        log.info("generateCertificatePdf - userInfo: {}", userInfoRes);
        if (userInfoRes == null) throw new EntityNotFoundException("유저 정보 없음");

        try (InputStream fontStream = new ClassPathResource("nanum-gothic/NanumGothic.ttf").getInputStream()){
            return generatePdf(certificate, userInfoRes, fontStream);
        } catch (IOException e) {
            throw new RuntimeException("PDF 생성 실패", e);
        }
    }

    @Override
    public byte[] generatePdf(Certificate certificate, UserFeignResDto userInfo, InputStream fontStream) throws IOException {
        log.info("=== PDF 생성 시작 ===");
        log.info("증명서 정보: {}", certificate);
        log.info("사용자 정보: {}", userInfo);

        try (PDDocument document = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            log.info("PDF 문서 생성 완료");

            PDPage page = new PDPage();
            document.addPage(page);
            log.info("PDF 페이지 추가 완료");

            log.info("폰트 로딩 시작");
            PDFont font = PDType0Font.load(document, fontStream, true);
            log.info("폰트 로딩 완료");

            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();
            log.info("페이지 크기: {} x {}", pageWidth, pageHeight);

            // 1. 표 위치 및 크기(A4 기준) - 너비 더 넓게
            float tableX = 40;
            float tableY = pageHeight - 180; // 위쪽에 배치
            float tableWidth = pageWidth - 80; // (기존 120 → 80)
            float rowHeight = 44;
            log.info("표 위치: x={}, y={}, width={}, height={}", tableX, tableY, tableWidth, rowHeight);

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
            String dept = (userInfo.getDepartment() != null) ? safe(userInfo.getDepartment().getName()) : "";
            String position = safe(userInfo.getPositionName());
            String period = userInfo.getHireDate() != null ? userInfo.getHireDate() + " ~ 현재" : "";
            String purpose = safe(certificate.getPurpose());



            log.info("추출된 데이터: userName={}, dept={}, position={}, period={}, purpose={}",
                    userName, dept, position, period, purpose);

            String title = "재직증명서";
            if (certificate.getType() != null) {
                title = switch (certificate.getType().name()) {
                    case "EMPLOYMENT" -> "재직증명서";
                    case "CAREER" -> "경력증명서";
                    default -> certificate.getType().name();
                };
            }
            log.info("증명서 제목: {}", title);

            log.info("PDF 콘텐츠 스트림 시작");
            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                // 1. 타이틀
                log.info("타이틀 그리기 시작");
                cs.beginText();
                cs.setFont(font, 36);
                float titleWidth = font.getStringWidth(title) / 1000 * 36;
                cs.newLineAtOffset((pageWidth - titleWidth) / 2, tableY + 60);
                cs.showText(title);
                cs.endText();
                log.info("타이틀 그리기 완료");

                // 2. 표 외곽선(4행)
                log.info("표 외곽선 그리기 시작");
                cs.setLineWidth(1.3f);
                cs.addRect(tableX, tableY - rowHeight * 4, tableWidth, rowHeight * 4);
                cs.stroke();
                log.info("표 외곽선 그리기 완료");

                // 3. 가로선
                log.info("가로선 그리기 시작");
                for (int i = 1; i < 4; i++) {
                    cs.moveTo(tableX, tableY - rowHeight * i);
                    cs.lineTo(tableX + tableWidth, tableY - rowHeight * i);
                    cs.stroke();
                }
                log.info("가로선 그리기 완료");

                // 4. 세로선 (1,3행만 네 칸)
                log.info("세로선 그리기 시작");
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
                log.info("세로선 그리기 완료");

                // 5. 표 텍스트 (폰트 20)
                log.info("표 텍스트 그리기 시작");
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
                log.info("표 텍스트 그리기 완료");

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
                log.info("하단 문구: {}", footerText);

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
                log.info("회사 정보 그리기 시작");
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
                log.info("도장 이미지 로딩 시작");
                try {
                    ClassPathResource resource = new ClassPathResource("9a94a2dad35c7ace.png");
                    byte[] sealBytes = resource.getInputStream().readAllBytes();
                    PDImageXObject sealImage = PDImageXObject.createFromByteArray(document, sealBytes, "seal");
                    log.info("도장 이미지 로딩 완료: 크기={} bytes", sealBytes.length);

                    // (인) 텍스트 중앙에 도장 중앙이 오도록, 크기 65x65
                    float sealWidth = 85;
                    float sealHeight = 85;
                    float inTextFontSize = 14;
                    float inTextWidth = font.getStringWidth("(인)") / 1000 * inTextFontSize;
                    float inTextHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * inTextFontSize;
                    float sealX = inTextX + inTextWidth/2 - sealWidth/2;
                    float sealY = inTextY + inTextHeight/2 - sealHeight/2;

                    cs.drawImage(sealImage, sealX, sealY, sealWidth, sealHeight);
                    log.info("도장 이미지 그리기 완료");
                } catch (Exception e) {
                    log.error("도장 이미지 로딩 실패: {}", e.getMessage());
                    // 도장 이미지가 없어도 PDF는 생성되도록 함
                }

                log.info("회사 정보 그리기 완료");
            }

            log.info("PDF 문서 저장 시작");
            document.save(baos);
            byte[] result = baos.toByteArray();
            log.info("PDF 생성 완료: 크기={} bytes", result.length);
            return result;
        } catch (Exception e) {
            log.error("PDF 생성 중 오류: {}", e.getMessage(), e);
            throw e;
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

    // [내부호출] 증명서 승인 처리
    @Override
    @Transactional
    public void approveCertificateInternal(Long id, Long approverId, String approverName) {
        Certificate certificate = certificateRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Certificate not found with id: " + id)
        );

        certificate.approve(approverId, approverName);
        certificate.setApproveDate(java.time.LocalDate.now());

        certificateRepository.save(certificate);
        log.info("Certificate {} has been approved by {}.".formatted(id, approverName));
    }

    // [내부호출] 증명서 반려 처리
    @Override
    @Transactional
    public void rejectCertificateInternal(Long id, String rejectComment, Long approverId, String approverName) {
        Certificate certificate = certificateRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Certificate not found with id: " + id)
        );

        // Certificate 엔티티의 reject 메서드 호출
        certificate.reject(approverId, rejectComment, approverName);
        // certificate.setRejectComment(rejectComment); // reject 메서드에서 이미 처리하므로 이 라인은 제거

        certificateRepository.save(certificate);
        log.info("Certificate {} has been rejected by {} with comment: {}.".formatted(id, approverName, rejectComment));
    }

    // 모든 증명서 조회 (HR 전용)
    @Override
    public Page<CertificateResDto> listAllCertificates(TokenUserInfo userInfo, Long employeeNo, Pageable pageable) {
        // HR 역할 검증
        if (!"Y".equals(userInfo.getHrRole())) {
            throw new IllegalStateException("Only HR users can view all certificates.");
        }

        Page<Certificate> certificates;
        if (employeeNo != null) {
            // employeeNo가 제공되면 해당 사원의 증명서만 조회
            certificates = certificateRepository.findByEmployeeNo(employeeNo, pageable);
        } else {
            // employeeNo가 제공되지 않으면 모든 증명서 조회
            certificates = certificateRepository.findAll(pageable);
        }

        // 모든 신청자 및 결재자 ID 추출
        List<Long> allUserIds = Stream.concat(
                certificates.stream().map(Certificate::getEmployeeNo),
                certificates.stream()
                        .filter(cert -> cert.getApprovalRequestId() != null)
                        .map(cert -> {
                            try {
                                ApprovalRequestResponseDto approvalResponse = approvalServiceClient.getApprovalRequestById(cert.getApprovalRequestId());
                                // [수정] 요청 타입이 'CERTIFICATE'인 경우에만 ID를 반환합니다.
                                if (approvalResponse != null && "CERTIFICATE".equals(approvalResponse.getRequestType())) {
                                    return approvalResponse.getApproverId();
                                } else {
                                    log.warn("조회된 결재 요청(ID: {})이 증명서 타입이 아닙니다. (타입: {})",
                                            cert.getApprovalRequestId(), approvalResponse != null ? approvalResponse.getRequestType() : "null");
                                    return null;
                                }
                            } catch (FeignException e) {
                                if (e.status() == 404) {
                                    log.warn("결재 서비스에서 ID {}에 해당하는 결재 요청을 찾을 수 없습니다.", cert.getApprovalRequestId());
                                } else {
                                    log.error("결재 서비스 통신 오류 (getApprovalRequestById) for approvalRequestId {}: {}", cert.getApprovalRequestId(), e.getMessage());
                                }
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
        ).distinct().collect(Collectors.toList());

        // HR 서비스에서 모든 사용자 정보 조회
        final Map<Long, UserResDto> userMap = new HashMap<>();
        try {
            List<UserResDto> usersInfo = hrServiceClient.getUsersInfo(allUserIds);
            if (usersInfo != null && !usersInfo.isEmpty()) {
                userMap.putAll(usersInfo.stream()
                        .collect(Collectors.toMap(UserResDto::getEmployeeNo, Function.identity())));
            }
        } catch (FeignException e) {
            log.error("HR 서비스 통신 오류 (getUsersInfo): {}", e.getMessage());
        }

        return certificates.map(certificate -> {
            String applicantName = null;
            String departmentName = null;
            String approverName = null;

            UserResDto applicantInfo = userMap.get(certificate.getEmployeeNo());
            if (applicantInfo != null) {
                applicantName = applicantInfo.getUserName();
                if (applicantInfo.getDepartment() != null) {
                    departmentName = applicantInfo.getDepartment().getName();
                }
            }

            if (certificate.getApprovalRequestId() != null) {
                try {
                    ApprovalRequestResponseDto approvalResponse = approvalServiceClient.getApprovalRequestById(certificate.getApprovalRequestId());
                    // [수정] 요청 타입이 'CERTIFICATE'인 경우에만 결재자 정보를 사용합니다.
                    if (approvalResponse != null && "CERTIFICATE".equals(approvalResponse.getRequestType())) {
                        log.info("ApprovalRequestResponseDto from approval-service (listAllCertificates): approverId={}, approverName={}", approvalResponse.getApproverId(), approvalResponse.getApproverName());
                        if (approvalResponse.getApproverId() != null) {
                            UserResDto approverInfo = userMap.get(approvalResponse.getApproverId());
                            if (approverInfo != null) {
                                approverName = approverInfo.getUserName();
                            } else {
                                // userMap에 정보가 없는 경우, 결재 응답에 이름이 있으면 사용
                                approverName = approvalResponse.getApproverName();
                                log.warn("HR 서비스에서 approverId {} 에 해당하는 사용자 정보를 찾을 수 없어 결재 응답의 이름을 사용합니다.", approvalResponse.getApproverId());
                            }
                        } else {
                            log.warn("ApprovalRequestResponseDto에서 approverId가 null입니다. approvalRequestId: {}", certificate.getApprovalRequestId());
                        }
                    } else {
                        log.warn("조회된 결재 요청(ID: {})이 증명서 타입이 아닙니다. (타입: {})",
                                certificate.getApprovalRequestId(), approvalResponse != null ? approvalResponse.getRequestType() : "null");
                    }
                } catch (FeignException e) {
                    if (e.status() == 404) {
                        log.warn("결재 서비스에서 ID {}에 해당하는 결재 요청을 찾을 수 없습니다.", certificate.getApprovalRequestId());
                    } else {
                        log.error("결재 서비스 통신 오류 (getApprovalRequestById) for approvalRequestId {}: {}", certificate.getApprovalRequestId(), e.getMessage());
                    }
                }
            }

            return CertificateResDto.builder()
                    .certificateId(certificate.getCertificateId())
                    .employeeNo(certificate.getEmployeeNo())
                    .type(Type.valueOf(certificate.getType().name()))
                    .requestDate(certificate.getRequestDate())
                    .approveDate(certificate.getApproveDate())
                    .processedAt(certificate.getProcessedAt())
                    .status(Status.valueOf(certificate.getStatus().name()))
                    .reason(certificate.getPurpose())
                    .applicantName(applicantName)
                    .departmentName(departmentName)
                    .approverName(certificate.getApproverName())
                    .rejectComment(certificate.getRejectComment())
                    .build();
        });
    }

    // 증명서 승인 (HR 전용)
    @Override
    @Transactional
    public void approveCertificate(Long id, TokenUserInfo userInfo) {
        Certificate certificate = certificateRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Certificate not found with id: " + id)
        );

        // HR 역할 검증 (선택 사항: 컨트롤러나 서비스 상위에서 이미 검증될 수 있음)
        if (!"Y".equals(userInfo.getHrRole())) {
            throw new IllegalStateException("Only HR users can approve certificates.");
        }

        // 이미 처리된 증명서인지 확인
        if (certificate.getStatus() != Status.PENDING) {
            throw new IllegalStateException("Only requested certificates can be approved. Current status: " + certificate.getStatus() + ")");
        }

        // approval-service를 다시 호출하는 대신, approveCertificateInternal을 직접 호출
        try {
            // HR 서비스에서 승인자 이름 조회
            String approverName = hrServiceClient.getUserById(userInfo.getEmployeeNo())
                    .getResult().getUserName();
            approveCertificateInternal(id, userInfo.getEmployeeNo(), approverName);
            log.info("Certificate {} has been approved by HR user {}.", id, userInfo.getEmployeeNo());
        } catch (Exception e) {
            log.error("증명서 승인 처리 중 예상치 못한 오류 발생 (certificateId: {}): {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "증명서 승인 처리 중 오류 발생", e);
        }
    }

    // 증명서 반려 (HR 전용)
    @Override
    @Transactional
    public void rejectCertificate(Long id, TokenUserInfo userInfo, CertificateRejectRequestDto rejectDto) {
        Certificate certificate = certificateRepository.findById(id).orElseThrow(
                () -> new EntityNotFoundException("Certificate not found with id: " + id)
        );

        // HR 역할 검증
        if (!"Y".equals(userInfo.getHrRole())) {
            throw new IllegalStateException("Only HR users can reject certificates.");
        }

        // 이미 처리된 증명서인지 확인
        if (certificate.getStatus() != Status.PENDING) {
            throw new IllegalStateException("Only requested certificates can be rejected. Current status: " + certificate.getStatus() + ")");
        }

        try {
            Long approvalRequestId = certificate.getApprovalRequestId();
            if (approvalRequestId == null) {
                throw new IllegalStateException("Approval request ID not found for certificate: " + id);
            }

            // approval-service의 rejectApprovalRequest API 호출
            com.playdata.certificateservice.client.dto.ApprovalRejectRequestDto approvalRejectRequestDto = new com.playdata.certificateservice.client.dto.ApprovalRejectRequestDto();
            approvalRejectRequestDto.setRejectComment(rejectDto.getRejectComment());
            ApprovalRequestResponseDto updatedApprovalRequest = approvalServiceClient.rejectApprovalRequest(approvalRequestId, userInfo.getEmployeeNo(), approvalRejectRequestDto);

            // Certificate 엔티티 업데이트
            // HR 서비스에서 반려자 이름 조회
            String approverName = hrServiceClient.getUserById(userInfo.getEmployeeNo())
                    .getResult().getUserName();
            certificate.reject(userInfo.getEmployeeNo(), rejectDto.getRejectComment(), approverName);

            certificateRepository.save(certificate);
            log.info("Certificate {} has been rejected by HR user {} with comment: {}. Approval Request ID: {}", id, userInfo.getEmployeeNo(), rejectDto.getRejectComment(), approvalRequestId);

        } catch (FeignException e) {
            log.error("ApprovalService 결재 반려 요청 실패 (certificateId: {}, approverId: {}): {}", id, userInfo.getEmployeeNo(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "결재 서비스 반려 처리 중 오류 발생", e);
        } catch (Exception e) {
            log.error("증명서 반려 처리 중 예상치 못한 오류 발생 (certificateId: {}): {}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "증명서 반려 처리 중 오류 발생", e);
        }
    }

    @Override
    public Certificate getCertificateById(Long id) {
        return certificateRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Certificate not found with id: " + id));
    }
}