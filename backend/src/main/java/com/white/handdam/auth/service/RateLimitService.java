package com.white.handdam.auth.service;

import com.white.handdam.auth.entity.VerificationPurpose;
import com.white.handdam.auth.exception.AuthErrorCode;
import com.white.handdam.auth.util.EmailNormalizer;
import com.white.handdam.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RateLimitService {

    private static final Duration COOLDOWN = Duration.ofSeconds(60); // 60초 쿨타임
    private static final Duration DAILY_WINDOW = Duration.ofDays(1);
    private static final int DAILY_LIMIT = 5; // 하루 최대 5회 시도 가능

    private final StringRedisTemplate redisTemplate;

    // 60초 쿨다운 - 동일 목적 메일 60초 이내 다시 요청 방지
    public void checkCooldown(String email, VerificationPurpose purpose) {
        String key = cooldownKey(email, purpose);

        Boolean success = redisTemplate.opsForValue() // Redis String 타입 데이터 처리
                .setIfAbsent(key, "1", COOLDOWN); // 키가 없을 때만 저장 (key, value, ttl)

        if (Boolean.FALSE.equals(success)) {
            throw new CustomException(AuthErrorCode.MAIL_SEND_RATE_LIMIT_EXCEEDED);
        }
    }

    // 일일 발송 횟수 증가 + 제한 초과 확인
    public void increaseDailyCount(String email, VerificationPurpose purpose) {
        String key = dailyKey(email, purpose);

        Long count = redisTemplate.opsForValue().increment(key); // 횟수 증가
        
        // 첫 요청이면 TTL 설정 (24시간)
        if (count == 1L) {
            redisTemplate.expire(key, DAILY_WINDOW);
        }
        if (count > DAILY_LIMIT) {
            throw new CustomException(AuthErrorCode.MAIL_SEND_RATE_LIMIT_EXCEEDED);
        }
    }

    // 쿨다운 키 생성
    private String cooldownKey(String email, VerificationPurpose purpose) {
        return "mail:cooldown:" + purpose.name() + ":" + EmailNormalizer.normalize(email);
    }

    // 일일 발송 횟수 키 생성
    private String dailyKey(String email, VerificationPurpose purpose) {
        return "mail:daily:" + purpose.name() + ":" + EmailNormalizer.normalize(email);
    }

}
