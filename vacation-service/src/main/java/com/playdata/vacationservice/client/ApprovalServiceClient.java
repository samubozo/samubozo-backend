    package com.playdata.vacationservice.client;

import com.playdata.vacationservice.client.dto.ApprovalRequestResponseDto;
import com.playdata.vacationservice.client.dto.ApprovalRequestDto;
import com.playdata.vacationservice.common.auth.TokenUserInfo;
import com.playdata.vacationservice.common.configs.FeignClientConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 결재 서비스와 통신하기 위한 Feign 클라이언트 인터페이스입니다.
 */
@FeignClient(name = "approval-service", configuration = FeignClientConfig .class)
public interface ApprovalServiceClient {

    /**
     * 결재 서비스에 새로운 결재 생성을 요청합니다.
     *
     * @param requestDto 결재 생성에 필요한 정보
     * @return 생성된 결재 요청의 응답 DTO
     */
    @PostMapping("/approvals/vacation")
    ApprovalRequestResponseDto createApproval(@RequestBody com.playdata.vacationservice.client.dto.VacationApprovalRequestCreateDto requestDto);

    /**
     * 특정 결재 요청을 승인 처리합니다.
     *
     * @param id 승인할 결재 요청의 ID
     * @param userInfo 승인자의 정보를 담은 객체 (Feign Client에서는 헤더로 전달)
     * @return 승인 처리된 결재 요청의 응답 DTO
     */
    @PutMapping("/approvals/{id}/approve")
    ApprovalRequestResponseDto approveApprovalRequest(@PathVariable("id") Long id, @RequestBody Object userInfo); // userInfo는 Feign Interceptor로 전달되므로 Object로 받음

    /**
     * 특정 결재 요청을 반려 처리합니다.
     *
     * @param id 반려할 결재 요청의 ID
     * @param userInfo 반려자의 정보를 담은 객체 (Feign Client에서는 헤더로 전달)
     * @return 반려 처리된 결재 요청의 응답 DTO
     */
    @PutMapping("/approvals/{id}/reject")
    ApprovalRequestResponseDto rejectApprovalRequest(@PathVariable("id") Long id, @RequestBody Object userInfo); // userInfo는 Feign Interceptor로 전달되므로 Object로 받음

    /**
     * 특정 결재 요청을 ID로 조회합니다.
     *
     * @param id 조회할 결재 요청의 ID
     * @return 조회된 결재 요청의 응답 DTO
     */
    @GetMapping("/approvals/{id}")
    ApprovalRequestResponseDto getApprovalRequestById(@PathVariable("id") Long id);

    /**
     * 특정 결재자가 처리한 모든 결재 요청 목록을 조회합니다.
     *
     * @param userInfo 인증된 사용자 정보 (결재자 ID 추출용)
     * @return 처리된 결재 요청 DTO 목록
     */
    @GetMapping("/approvals/processed-by-me")
    List<ApprovalRequestResponseDto> getProcessedApprovalRequestsByApproverId(@RequestBody TokenUserInfo userInfo);
}