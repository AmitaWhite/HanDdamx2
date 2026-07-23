package com.white.handdam.global.security.jwt;

import com.white.handdam.member.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class JwtTokenPrinterTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void printAccessToken() {
        String token = jwtTokenProvider.createAccessToken(1L, Role.USER);
        System.out.println("access token (USER, id=1) = " + token);
    }

    @Test
    void printAdminAccessToken() {
        String token = jwtTokenProvider.createAccessToken(1L, Role.ADMIN);
        System.out.println("access token (ADMIN, id=1) = " + token);
    }
}
