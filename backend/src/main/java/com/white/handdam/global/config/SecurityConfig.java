package com.white.handdam.global.config;

import com.white.handdam.auth.oauth.CustomOAuth2UserService;
import com.white.handdam.auth.oauth.OAuth2FailureHandler;
import com.white.handdam.auth.oauth.OAuth2SuccessHandler;
import com.white.handdam.global.security.jwt.JwtAccessDeniedHandler;
import com.white.handdam.global.security.jwt.JwtAuthenticationEntryPoint;
import com.white.handdam.global.security.jwt.JwtAuthenticationFilter;
import com.white.handdam.global.security.jwt.JwtProperties;
import com.white.handdam.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

        // auth 도메인에 로그인/재발급 등 신규 공개 엔드포인트가 추가되면 이 목록도 같이 갱신해야 한다.
        private static final String[] PERMIT_ALL = {
                        "/api/auth/signup",
                        "/api/auth/email-availability",
                        "/api/auth/nickname-availability",
                        "/api/auth/email-verifications/**",
                        "/api/auth/login",
                        "/api/auth/token/refresh",
                        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                        "/actuator/health",
                        // handshake만 공개. STOMP CONNECT JWT는 StompAuthChannelInterceptor에서 검증
                        "/ws",
                        "/ws/**",
                        "/oauth2/**",
                        "/login/oauth2/**",
                        "/api/feeds/explore",
                        "/api/feeds/creators/**",
                        "/api/feeds/public",
                        "/api/feeds/*",
                        "/api/feeds/*/comments",
                        "/api/categories",
                        "/api/creators/*/projects",
        };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http.csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> {
            })
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(PERMIT_ALL).permitAll()
                .requestMatchers(HttpMethod.GET, "/api/members/{memberId:[0-9]+}").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/projects/*").permitAll()         // 상세 조회만 공개
                .requestMatchers(HttpMethod.GET, "/api/projects/*/feeds").permitAll()   // 피드 목록만 공개
                .requestMatchers(HttpMethod.GET, "/api/feeds/me").hasRole("CREATOR")    // 크리에이터만 접근가능
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                .accessDeniedHandler(jwtAccessDeniedHandler))
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                .successHandler(oAuth2SuccessHandler)
                .failureHandler(oAuth2FailureHandler))
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                UsernamePasswordAuthenticationFilter.class)
            .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
