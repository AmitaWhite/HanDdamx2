package com.white.handdam.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPI 공통 설정.
 *
 * <p>
 * API 메타정보(title/version/description)와 JWT Bearer 인증 스킴을 전역에 등록한다.
 * 이 설정 덕분에 Swagger UI 우측 상단에 <b>Authorize</b> 버튼이 나타나고,
 * 로그인으로 발급받은 access token을 한 번 입력하면 이후 모든 "Try it out" 요청에
 * {@code Authorization: Bearer <token>} 헤더가 자동으로 붙는다.
 *
 * <p>
 * 실제 토큰 검증은 {@code JwtAuthenticationFilter}가 담당하며, 여기서는 문서/테스트 UI에
 * 인증 입력 수단을 노출하는 역할만 한다.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI handdamOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("HanDdam(한땀한땀) API")
                        .description("공예품 DIY 제작자 구독 플랫폼 백엔드 API 문서")
                        .version("v0.0.1"))
                // 모든 API에 기본 인증 요구를 걸어 UI에서 Authorize 값을 재사용하게 한다.
                // (permitAll 엔드포인트는 서버 SecurityConfig 기준으로 실제 통과된다.)
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
