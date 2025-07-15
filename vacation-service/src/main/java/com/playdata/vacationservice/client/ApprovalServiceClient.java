    package com.playdata.vacationservice.client;

    import com.playdata.vacationservice.client.dto.ApprovalRequestDto; // 경로 변경
    import com.playdata.vacationservice.common.configs.FeignClientConfig;
    import org.springframework.cloud.openfeign.FeignClient;
    import org.springframework.web.bind.annotation.PostMapping;
    import org.springframework.web.bind.annotation.RequestBody;

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
    }