package com.playdata.authservice.auth.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailSenderServiceImpl implements MailSenderService {

    private final JavaMailSender mailSender;

    @Override
    public String joinMail(String email) throws MessagingException {
        int authNum = makeRandomNumber();
        String setFrom = "gh939@naver.com";
        String title = "SAMUBOZO ERP 비밀번호 재설정 인증 코드 안내";
        String content = "<strong>[사무보조 ERP 회원가입 인증번호 안내]</strong><br><br>" +
                        "안녕하세요.<br>" +
                        "아래 인증 코드를 입력하시면 비밀번호 재설정을 진행하실 수 있습니다.<br><br>" +
                        "아래 인증번호를 화면에 입력해 주세요.<br><br>" +
                        "<strong>인증번호: " + authNum + "</strong><br><br>" +
                        "- 이 코드는 발송 시점부터 5분간 유효합니다.<br>" +
                        "- 본 메일은 사내 ERP 비밀번호 찾기 신청 시 자동 발송됩니다.<br><br>" +
                        "사무보조 ERP 시스템 드림";
        mailSend(setFrom, email, title, content);

        return Integer.toString(authNum);
    }

    @Override
    public void mailSend(String setFrom, String toMail, String title, String content) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "utf-8");
        mimeMessageHelper.setFrom(setFrom);
        mimeMessageHelper.setTo(toMail);
        mimeMessageHelper.setSubject(title);
        mimeMessageHelper.setText(content, true);

        mailSender.send(mimeMessage);
    }

    @Override
    public int makeRandomNumber() {
        int v =(int) ((Math.random() * 999999) + 111111);
        log.info("check number is {}", v);
        return v;
    }

    @Override
    public void sendPasswordResetMail(@NotBlank(message = "이메일을 입력해 주세요.") String email, String userName, String code) throws MessagingException {
        String subject = "[SAMUBOZO ERP] 비밀번호 재설정 인증 코드 안내";
        String content = userName + "님, 안녕하세요!<br><br>" +
                "아래 인증 코드를 입력하시면 비밀번호 재설정을 진행하실 수 있습니다.<br>" +
                "<strong>인증번호: " + code + "</strong><br><br>" +
                "- 이 코드는 발송 시점부터 5분간 유효합니다.<br>" +
                "- 본 메일은 사내 ERP 비밀번호 찾기 신청 시 자동 발송됩니다.<br><br>" +
                "사무보조 ERP 시스템 드림";

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "utf-8");
        helper.setFrom("gh939@naver.com");
        helper.setTo(email);
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

}
