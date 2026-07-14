package com.white.handdam.auth.mail;

// 회원가입 / 비밀번호 변경 이메일 인증 형식 구분 위함
public record MailContent(
        String subject,
        String body
) {
}
