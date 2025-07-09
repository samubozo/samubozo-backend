package com.playdata.certificateservice.repository;

import com.playdata.certificateservice.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

}
