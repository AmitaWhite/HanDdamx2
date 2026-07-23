package com.white.handdam.auth.oauth;

import com.white.handdam.global.security.jwt.JwtProperties;
import com.white.handdam.global.security.jwt.JwtTokenProvider;
import com.white.handdam.global.security.jwt.RefreshTokenRepository;
import com.white.handdam.member.entity.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.frontend-base-url}")
    private String frontendUrl;

    @Value("${app.cookie-secure}")
    private boolean cookieSecure;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User principal = (CustomOAuth2User) authentication.getPrincipal();

        Long memberId = principal.getMemberId();
        Role role = principal.getRole();

        String refreshToken = jwtTokenProvider.createRefreshToken(memberId, role);

        refreshTokenRepository.save(memberId, refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(cookieSecure) // local=false, 배포/prod=true
                .sameSite("Strict")
                .path("/")
                .maxAge(jwtProperties.refreshExpiration() / 1000)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        // 프론트에서 refreshToken 쿠키로 /api/auth/token/refresh 호출해서 authToken 발급 필요
        response.sendRedirect(frontendUrl + "/oauth/callback");
    }
}
