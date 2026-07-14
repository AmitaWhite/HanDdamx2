package com.white.handdam.auth.event;

import com.white.handdam.auth.mail.MailContent;
import com.white.handdam.auth.mail.SmtpEmailSender;
import com.white.handdam.auth.mail.VerificationEmailContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class VerificationEmailEventListener {

    private final SmtpEmailSender emailSender;
    private final VerificationEmailContent verificationEmailContent;

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(VerificationEmailRequestedEvent event) {
        try {
            MailContent content = verificationEmailContent.create(
                    event.nickname(),
                    event.rawToken()
            );

            emailSender.send(
                    event.email(),
                    content.subject(),
                    content.body()
            );
        } catch (Exception e) {
            log.error("인증메일 발송 실패 : email={}, purpose={}", event.email(), event.purpose(), e);
        }
    }

}