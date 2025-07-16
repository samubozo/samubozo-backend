    package com.playdata.vacationservice.client;

    import com.playdata.vacationservice.client.dto.ApprovalRequestDto; // 경로 변경
    import com.playdata.vacationservice.common.configs.FeignClientConfig;
    import org.springframework.cloud.openfeign.FeignClient;
    import org.springframework.web.bind.annotation.PathVariable;
    import org.springframework.web.bind.annotation.PostMapping;
    import org.springframework.web.bind.annotation.PutMapping;
    import org.springframework.web.bind.annotation.RequestBody;
    import org.springframework.web.bind.annotation.RequestParam;

    /**
     * 결재 서비스와 통신하기 위한 Feign 클라이언트 인터페이스입니다.
     */
    @FeignClient(name = "approval-service", configuration = FeignClientConfig .class) // configuration 속성 추가
    public interface ApprovalServiceClient {

        /**
         * 결재 서비스에 새로운 결재 생성을 요청합니다.
         *
         * @param requestDto 결재 생성에 필요한 정보
         */
        @PostMapping("/approvals")
        void createApproval(@RequestBody ApprovalRequestDto requestDto);

        /**
         * 특정 결재의 상태를 업데이트합니다.
         *
         * @param approvalId 업데이트할 결재 ID
         * @param status 업데이트할 결재 상태 (APPROVED, REJECTED)
         * @param comment 반려 시 사유 (선택 사항)
         */
        @PutMapping("/approvals/{approvalId}/status")
        void updateApprovalStatus(@PathVariable("approvalId") Long approvalId,
                                  @RequestParam("status") String status,
                                  @RequestParam(value = "comment", required = false) String comment);
    }