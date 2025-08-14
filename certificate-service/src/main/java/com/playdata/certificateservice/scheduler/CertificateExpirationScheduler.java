package com.playdata.certificateservice.scheduler;

import com.playdata.certificateservice.entity.Certificate;
import com.playdata.certificateservice.entity.Status;
import com.playdata.certificateservice.repository.CertificateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CertificateExpirationScheduler {

    private final CertificateRepository certificateRepository;

    // 매일 자정(0시 0분 0초)에 실행
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void updateExpiredCertificates() {
        log.info("만료된 증명서 상태 업데이트 스케줄러 시작: {}", LocalDate.now());

        // 오늘 날짜를 기준으로 만료일이 지났고, APPROVED 상태인 증명서 조회
        List<Certificate> expiredCertificates = certificateRepository.findByExpirationDateBeforeAndStatus(
                LocalDate.now(), Status.APPROVED
        );

        if (expiredCertificates.isEmpty()) {
            log.info("만료된 증명서가 없습니다.");
            return;
        }

        log.info("만료된 증명서 {}건 발견. 상태 업데이트 시작.", expiredCertificates.size());

        for (Certificate certificate : expiredCertificates) {
            certificate.setStatus(Status.EXPIRED);
            certificateRepository.save(certificate);
            log.info("증명서 ID: {} 상태를 EXPIRED로 업데이트 완료.", certificate.getCertificateId());
        }

        log.info("만료된 증명서 상태 업데이트 스케줄러 완료.");
    }
}
