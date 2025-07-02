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
public class MailSenderService {

    // EmailConfig 에 선언한 메일 전송 핵심객체 주입받기
    private final JavaMailSender mailSender;

    //가입할 회원에게 전송할 이메일 양식 준비
    // userService 가 호출할 메서드
    public String joinMail(String email) throws MessagingException {
        int authNum = makeRandomNumber();
        String setFrom = "gh939@naver.com"; // 발신용 이메일 주소 (yml과 동일하게)
        String toMail = email;
        String title = "SAMUBOZO ERP 회원가입 인증번호 안내";
        String content = "<strong>[사무보조 ERP 회원가입 인증번호 안내]</strong><br><br>" +
                        "안녕하세요.<br>" +
                        "사무보조 ERP 시스템 신규 계정 등록 요청이 접수되었습니다.<br><br>" +
                        "아래 인증번호를 회원가입 화면에 입력해 주세요.<br><br>" +
                        "<strong>인증번호: " + authNum + "</strong><br><br>" +
                        "- 인증번호는 30분간 유효합니다. 만료 시 재발급을 요청하세요.<br>" +
                        "- 본 메일은 사내 ERP 신규 계정 신청 시 자동 발송됩니다.<br>" +
                        "- 본인이 직접 요청하지 않은 경우, 사내 IT팀(it-support@samubozo.com)으로 문의 바랍니다.<br><br>" +
                        "사무보조 ERP 시스템 드림"; // 이메일에 삽입할 내용 (더 꾸며보세요)
        mailSend(setFrom,toMail, title, content);

        return Integer.toString(authNum);
    }

    // 여기서 실제 이메일이 전송
    private void mailSend(String setFrom, String toMail, String title, String content) throws MessagingException {
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

    private int makeRandomNumber() {
        //난수의 범위: 111111~999999
        int v =(int) ((Math.random() * 999999) + 111111);
        log.info("check number is {}", v);
        return v;
    }

    public void sendPasswordResetMail(@NotBlank(message = "이메일을 입력해 주세요.") String email, String userName, String code) throws MessagingException {
        String subject = "[YourApp] 비밀번호 재설정 인증 코드 안내";
        String content = new StringBuilder()
                .append(userName).append("님, 안녕하세요!<br><br>")
                .append("아래 인증 코드를 입력하시면 비밀번호 재설정을 진행하실 수 있습니다.<br>")
                .append("<strong>").append(code).append("</strong><br><br>")
                .append("이 코드는 발송 시점부터 5분간 유효합니다.")
                .toString();

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, false, "utf-8");
        helper.setFrom("uiuo1266@gmail.com");
        helper.setTo(email);
        helper.setSubject(subject);
        helper.setText(content, true);

        mailSender.send(message);
    }

}
