package com.playdata.certificateservice.client;

import com.playdata.certificateservice.client.dto.ApprovalRequestCreateDto;
import com.playdata.certificateservice.client.dto.ApprovalRequestResponseDto;
import com.playdata.certificateservice.client.dto.CertificateApprovalRequestCreateDto;
import com.playdata.certificateservice.client.dto.ApprovalRejectRequestDto; // 추가
import com.playdata.certificateservice.common.auth.TokenUserInfo; // 추가
import com.playdata.certificateservice.common.configs.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "approval-service", configuration = FeignClientConfig.class)
public interface ApprovalServiceClient {

    @PostMapping("/approvals")
    void createApprovalRequest(@RequestBody ApprovalRequestCreateDto createDto);

    @PostMapping("/approvals/certificate")
    ApprovalRequestResponseDto createCertificateApprovalRequest(@RequestBody CertificateApprovalRequestCreateDto createDto);

    @GetMapping("/approvals/{id}")
    ApprovalRequestResponseDto getApprovalRequestById(@PathVariable("id") Long id);

    @PutMapping("/approvals/{id}/approve")
    ApprovalRequestResponseDto approveApprovalRequest(@PathVariable("id") Long id, @RequestHeader("X-User-Employee-No") Long employeeNo);

    @PutMapping("/approvals/{id}/reject")
    ApprovalRequestResponseDto rejectApprovalRequest(@PathVariable("id") Long id, @RequestHeader("X-User-Employee-No") Long employeeNo, @RequestBody ApprovalRejectRequestDto rejectDto);
}
