package com.playdata.vacationservice.common.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class EmailConfig {
    @Value("${spring.mail.host}")
    private String host;
    @Value("${spring.mail.port}")
    private int port;
    @Value("${spring.mail.username}")
    private String username;
    @Value("${spring.mail.password}")
    private String password;
    @Value("${spring.mail.properties.mail.smtp.auth}")
    private boolean auth;
    //구글일 때 쓰기
//    @Value("${spring.mail.properties.mail.smtp.starttls.enable}")
//    private boolean starttlsEnable;
    @Value("${spring.mail.properties.mail.smtp.ssl.enable}")
    private boolean sslEnable;
    @Value("${spring.mail.properties.mail.smtp.ssl.trust}")
    private String sslTrust;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setDefaultEncoding("UTF-8");

        Properties properties = new Properties();
        properties.put("mail.smtp.auth", auth);
        //구글 일떄 쓰기
//        properties.put("mail.smtp.starttls.enable", starttlsEnable);
        properties.put("mail.smtp.ssl.enable", sslEnable);
        properties.put("mail.smtp.ssl.trust", sslTrust);

        mailSender.setJavaMailProperties(properties);
        return mailSender;
    }
}
