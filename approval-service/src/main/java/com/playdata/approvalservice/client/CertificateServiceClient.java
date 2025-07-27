package com.playdata.approvalservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "certificate-service")
public interface CertificateServiceClient {

    @PutMapping("/certificate/internal/certificates/{id}/approve")
    void approveCertificate(@PathVariable("id") Long id, @RequestParam("approverId") Long approverId, @RequestParam("approverName") String approverName);

    @PutMapping("/certificate/internal/certificates/{id}/reject")
    void rejectCertificateInternal(@PathVariable("id") Long id, @RequestParam("approverName") String approverName);

    @GetMapping("/certificate/{id}")
    com.playdata.approvalservice.client.dto.Certificate getCertificateById(@PathVariable("id") Long id);

}
