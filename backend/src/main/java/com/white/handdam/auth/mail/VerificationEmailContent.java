package com.white.handdam.auth.mail;

import com.white.handdam.auth.entity.VerificationPurpose;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VerificationEmailContent {

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    public MailContent create(String nickname, String rawToken, VerificationPurpose purpose) {
        return switch (purpose) {
            case SIGNUP -> signup(nickname, rawToken);
            case PASSWORD_RESET -> passwordReset(nickname, rawToken);
        };

    }

    private MailContent signup(String nickname, String rawToken) {
        String verifylink = frontendBaseUrl + "/email-verify?token=" + rawToken;


        return new MailContent(
                "[한땀한땀] 이메일 인증을 완료해주세요",
                """
                      <div>
                        <p>%s님, 가입을 환영합니다.</p><br><br>
                        <p>한땀한땀 회원가입을 위한 이메일 인증을 요청하셨습니다.</p><br>
                        <p>아래 버튼을 눌러 이메일 인증을 완료해주세요.</p><br><br>
                        <a href="%s">
                            이메일 인증하기
                        </a><br><br>
                        
                        <p>- 본인이 요청하지 않았다면 이 메일은 무시하셔도 됩니다.</p>
                        <p>- 인증 링크는 10분 동안 유효합니다.</p>
                      </div>  
                      """.formatted(nickname, verifylink)
        );
    }

    private MailContent passwordReset(String nickname, String rawToken) {
        String verifylink = frontendBaseUrl + "/reset-password?token=" + rawToken;


        return new MailContent(
                "[한땀한땀] 비밀번호 재설정 안내",
                """
                      <div>
                        <p>안녕하세요 %s님, </p><br>
                        
                        <p>한땀한땀 계정의 비밀번호 재설정을 요청하셨습니다.</p><br>
                        <p>아래 버튼을 눌러 새로운 비밀번호를 설정해주세요.</p>
                        <a href="%s">
                            비밀번호 재설정하기
                        </a><br><br>
                        
                        <p>- 본인이 요청하지 않았다면 이 메일은 무시하셔도 됩니다.</p>
                        <p>- 재설정 링크는 10분 동안 유효합니다.</p><br>
                      </div>  
                      """.formatted(nickname, verifylink)
        );
    }

}
