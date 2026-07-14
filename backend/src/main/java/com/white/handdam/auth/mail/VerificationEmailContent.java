package com.white.handdam.auth.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VerificationEmailContent {

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    public MailContent create(String nickname, String rawToken) {

        String verifylink = frontendBaseUrl + "/email-verify?token=" + rawToken;


        return new MailContent(
                "[한땀한땀] 이메일 인증을 완료해주세요",
                """
                      <div>
                        <p>%s님, 가입을 환영합니다.</p>
                        <p>아래 버튼을 눌러 이메일 인증을 완료해주세요.</p>
                        <a href="%s">
                            이메일 인증하기
                        </a>
                      </div>  
                      """.formatted(nickname, verifylink)
        );
    }

}
