package com.white.handdam.global.security.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "refresh-token:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    public void save(Long memberId, String refreshToken) {
        redisTemplate.opsForValue().set(
                KEY_PREFIX + memberId,
                refreshToken,
                Duration.ofMillis(jwtProperties.refreshExpiration()));
    }

    public Optional<String> findByMemberId(Long memberId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + memberId));
    }

    public void deleteByMemberId(Long memberId) {
        redisTemplate.delete(KEY_PREFIX + memberId);
    }
}
