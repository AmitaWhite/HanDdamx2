package com.white.handdam.auth.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmtpEmailSender {

    private final JavaMailSender mailSender;

    public void send(
            String to,
            String subject,
            String content
    ) {
        // 이메일 메시지 객체 생성
        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);

            log.info("메일 발송 성공");
        } catch (MessagingException e) {
            log.error("메일 발송 실패 : receiver={}", to, e);
            throw new MailSendException("메일 발송 실패", e);
        }
    }

}
