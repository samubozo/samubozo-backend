package com.playdata.authservice.auth.service;

import jakarta.mail.MessagingException;
import jakarta.validation.constraints.NotBlank;

public interface MailSenderService {

    String joinMail(String email) throws MessagingException;

    void mailSend(String setFrom, String toMail, String title, String content) throws MessagingException;

    int makeRandomNumber();

    void sendPasswordResetMail(@NotBlank(message = "이메일을 입력해 주세요.") String email, String userName, String code) throws MessagingException;

}
