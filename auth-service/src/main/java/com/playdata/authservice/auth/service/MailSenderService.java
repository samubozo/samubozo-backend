package com.playdata.authservice.auth.service;

import jakarta.mail.MessagingException;
import jakarta.validation.constraints.NotBlank;

public interface MailSenderService {
    //가입할 회원에게 전송할 이메일 양식 준비
    // userService 가 호출할 메서드
    String joinMail(String email) throws MessagingException;

    // 여기서 실제 이메일이 전송
    void mailSend(String setFrom, String toMail, String title, String content) throws MessagingException;

    int makeRandomNumber();

    void sendPasswordResetMail(@NotBlank(message = "이메일을 입력해 주세요.") String email, String userName, String code) throws MessagingException;
}
