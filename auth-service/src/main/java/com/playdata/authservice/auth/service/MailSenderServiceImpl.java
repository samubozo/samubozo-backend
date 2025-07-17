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

    // EmailConfig 에 선언한 메일 전송 핵심객체 주입받기
    private final JavaMailSender mailSender;

    //가입할 회원에게 전송할 이메일 양식 준비
    // userService 가 호출할 메서드
    @Override
    public String joinMail(String email) throws MessagingException {
        int authNum = makeRandomNumber();
        String setFrom = "gh939@naver.com"; // 발신용 이메일 주소 (yml과 동일하게)
        String title = "SAMUBOZO ERP 비밀번호 재설정 인증 코드 안내";
        String content = "<strong>[사무보조 ERP 회원가입 인증번호 안내]</strong><br><br>" +
                        "안녕하세요.<br>" +
                        "아래 인증 코드를 입력하시면 비밀번호 재설정을 진행하실 수 있습니다.<br><br>" +
                        "아래 인증번호를 화면에 입력해 주세요.<br><br>" +
                        "<strong>인증번호: " + authNum + "</strong><br><br>" +
                        "- 이 코드는 발송 시점부터 5분간 유효합니다.<br>" +
                        "- 본 메일은 사내 ERP 비밀번호 찾기 신청 시 자동 발송됩니다.<br><br>" +
                        "사무보조 ERP 시스템 드림"; // 이메일에 삽입할 내용 (더 꾸며보세요)
        mailSend(setFrom, email, title, content);

        return Integer.toString(authNum);
    }

    // 여기서 실제 이메일이 전송
    @Override
    public void mailSend(String setFrom, String toMail, String title, String content) throws MessagingException {
        // MimeMessage란 JavaMail 라이브러리에서 이메일 메세지를 나타내는 클래스. (생성, 설정, 수정, 전송 담당)
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        /*
            기타 설정들을 담당할 MimeMessageHelper 객체를 생성
            생성자의 매개값으로 MimeMessage 객체, bool, 문자 인코딩 설정
            true 매개값을 전달하면 MultiPart 형식의 메세지 전달이 가능 (첨부 파일)
        */
        MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage, false, "utf-8");
        mimeMessageHelper.setFrom(setFrom);
        mimeMessageHelper.setTo(toMail);
        mimeMessageHelper.setSubject(title);
        // 내용 채우기 true안하면 단순텍스트로 감
        mimeMessageHelper.setText(content, true);
        //메일 전
        mailSender.send(mimeMessage);
    }

    @Override
    public int makeRandomNumber() {
        //난수의 범위: 111111~999999
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
