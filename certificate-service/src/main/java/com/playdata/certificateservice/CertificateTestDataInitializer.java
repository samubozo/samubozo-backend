package com.playdata.certificateservice;

import com.playdata.certificateservice.entity.Certificate;
import com.playdata.certificateservice.entity.Status;
import com.playdata.certificateservice.entity.Type;
import com.playdata.certificateservice.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class CertificateTestDataInitializer implements CommandLineRunner {

    private final CertificateRepository certificateRepository;

    @Override
    public void run(String... args) {
        // 10건의 증명서 더미 데이터 생성 (APPROVED만 승인일자 입력)
        saveCertificate(1L, Type.EMPLOYMENT, Status.REQUESTED, "은행제출용", LocalDate.of(2025, 7, 10), null);
        saveCertificate(1L, Type.CAREER, Status.APPROVED, "이직용", LocalDate.of(2025, 7, 9), LocalDate.of(2025, 7, 10));
        saveCertificate(2L, Type.EMPLOYMENT, Status.REJECTED, "비자발급", LocalDate.of(2025, 7, 8), null);
        saveCertificate(2L, Type.CAREER, Status.REQUESTED, "해외연수", LocalDate.of(2025, 7, 7), null);
        saveCertificate(3L, Type.EMPLOYMENT, Status.APPROVED, "기타", LocalDate.of(2025, 7, 6), LocalDate.of(2025, 7, 7));
        saveCertificate(3L, Type.CAREER, Status.REQUESTED, "은행제출용", LocalDate.of(2025, 7, 5), null);
        saveCertificate(4L, Type.EMPLOYMENT, Status.APPROVED, "이직용", LocalDate.of(2025, 7, 4), LocalDate.of(2025, 7, 5));
        saveCertificate(5L, Type.CAREER, Status.REJECTED, "비자발급", LocalDate.of(2025, 7, 3), null);
        saveCertificate(5L, Type.EMPLOYMENT, Status.APPROVED, "해외연수", LocalDate.of(2025, 7, 2), LocalDate.of(2025, 7, 3));
        saveCertificate(2L, Type.CAREER, Status.REQUESTED, "기타", LocalDate.of(2025, 7, 1), null);

        log.info("증명서 더미 데이터 10건 생성 완료");
    }

    private void saveCertificate(Long employeeNo, Type type, Status status, String purpose, LocalDate requestDate, LocalDate approveDate) {
        Certificate certificate = Certificate.builder()
                .employeeNo(employeeNo)
                .type(type)
                .status(status)
                .purpose(purpose)
                .requestDate(requestDate)
                .approveDate(approveDate)
                .build();

        certificateRepository.save(certificate);
        log.info("더미 증명서 데이터 저장: {}", certificate);
    }
}