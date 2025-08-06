package com.playdata.certificateservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableFeignClients(basePackages = "com.playdata.certificateservice.client")
@EnableJpaAuditing
@EnableScheduling
public class CertificateServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CertificateServiceApplication.class, args);
    }

}
