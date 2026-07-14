package com.white.handdam.auth.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

// 이메일 인증용 랜덤 토큰 생성 및 해싱
public class TokenGenerator {

    // 난수 생성
    private static final SecureRandom RANDOM = new SecureRandom();

    // rawToken 생성
    public static String generateOpaqueToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes); // 배열에 랜덤값 채우기
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); // Base64 -> 문자열 -> 해시값 변경
    }

    // tokenHash 생성 - DB 저장용
    public static String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

}
