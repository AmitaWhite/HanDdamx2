package com.white.handdam.global.security.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

/**
 * DB/Redis 없이 JwtProperties 하나만 띄워서 local 프로필이 backend/.env의 JWT_SECRET을
 * 실제로 읽어오는지 확인하는 용도의 임시 테스트.
 */
@SpringBootTest(classes = JwtPropertiesLocalProfileTest.TestConfig.class)
@ActiveProfiles("local")
class JwtPropertiesLocalProfileTest {

    @Configuration
    @EnableConfigurationProperties(JwtProperties.class)
    static class TestConfig {
    }

    @Autowired
    private JwtProperties jwtProperties;

    @Test
    void printResolvedSecret() {
        System.out.println("resolved jwt.secret (local profile) = " + jwtProperties.secret());
    }
}
