package com.playdata.notificationservice.batch;

import com.playdata.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationBatchScheduler {

    private final NotificationRepository notificationRepository;

    // 매일 자정(0시 0분 0초)에 실행
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void deleteOldNotifications() {
        log.info("Starting batch job: deleteOldNotifications");
        LocalDateTime sixtyDaysAgo = LocalDateTime.now().minusDays(60);
        notificationRepository.deleteByCreatedAtBefore(sixtyDaysAgo);
        log.info("Finished batch job: deleteOldNotifications. Deleted notifications created before {}", sixtyDaysAgo);
    }
}
