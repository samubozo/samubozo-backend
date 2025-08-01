package com.playdata.payrollservice.common.configs;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling  // 💡 이것만 추가해도 스케줄러가 작동할 수 있게 됨
public class SchedulerConfig {
}