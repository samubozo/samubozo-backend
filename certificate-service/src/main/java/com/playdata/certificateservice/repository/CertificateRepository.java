package com.playdata.certificateservice.repository;


import com.playdata.certificateservice.entity.Certificate;
import com.playdata.certificateservice.entity.Status;
import com.playdata.certificateservice.entity.Type;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {

    Page<Certificate> findByEmployeeNo(Long employeeNo, Pageable pageable);

    Optional<Certificate> findByEmployeeNoAndTypeAndStatusAndExpirationDateAfter(Long employeeNo, Type type, Status status, LocalDate currentDate);

    List<Certificate> findByExpirationDateBeforeAndStatus(LocalDate expirationDate, Status status);
}
