package com.playdata.certificateservice.repository;


import com.playdata.certificateservice.entity.Certificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Page<Certificate> findByEmployeeNo(Long employeeNo, Pageable pageable);
}
