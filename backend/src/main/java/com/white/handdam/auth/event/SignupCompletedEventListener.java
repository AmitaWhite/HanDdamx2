package com.white.handdam.auth.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SignupCompletedEventListener {

    @Async("emailExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SignupCompletedEvent event) {
        log.info("[SIGNUP] 회원가입 인증 메일 발송 이벤트 (email={}, token={})",
                event.email(),
                event.rawToken()); // TODO: 추후 제거 필요 -> memberId로 변경

        // 이메일 발송 로직 추가
    }

}
