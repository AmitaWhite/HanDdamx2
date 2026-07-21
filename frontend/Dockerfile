# =========================================================
# HanDdamx2 frontend 이미지 (OCI ARM Ampere A1 = linux/arm64)
#
# 설계 의도:
#   - Vite 빌드(bun run build)는 CI의 x86 러너에서 네이티브로 수행한다(빠름).
#   - 산출물 dist/ 는 정적 파일이라 아키텍처 독립적 → 이 이미지는 nginx로 서빙만 한다.
#   - 따라서 arm64 이미지도 QEMU 에뮬레이션 없이 가볍게 만들어진다.
#
# 전제:
#   - docker build 이전에 `bun run build` 가 실행되어 frontend/dist 가 존재해야 한다.
#   - VITE_API_BASE_URL / VITE_WS_BASE_URL 은 "빌드 시점"에 번들로 구워지므로,
#     CI의 build 단계에서 프로덕션 도메인 값으로 주입해야 한다 (Dockerfile 단계 아님).
# =========================================================
FROM nginx:alpine

# 기본 서버 블록을 우리 SPA 설정으로 교체.
RUN rm /etc/nginx/conf.d/default.conf
COPY nginx.conf /etc/nginx/conf.d/handdam.conf

# CI가 미리 빌드한 정적 산출물 복사.
COPY dist /usr/share/nginx/html

EXPOSE 80

# nginx:alpine 기본 CMD(nginx -g 'daemon off;') 를 그대로 사용.
