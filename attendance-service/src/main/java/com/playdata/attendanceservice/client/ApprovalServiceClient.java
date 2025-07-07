package com.playdata.attendanceservice.client;

import com.playdata.attendanceservice.client.dto.ApprovalRequestDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 결재 서비스와 통신하기 위한 Feign 클라이언트 인터페이스입니다.
 */
@FeignClient(name = "approval-service")
public interface ApprovalServiceClient {

    /**
     * 결재 서비스에 새로운 결재 생성을 요청합니다.
     *
     * @param requestDto 결재 생성에 필요한 정보
     */
    @PostMapping("/api/v1/approvals")
    void createApproval(@RequestBody ApprovalRequestDto requestDto);
}
