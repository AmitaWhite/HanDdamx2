# =========================================================
# HanDdamx2 backend 이미지 (OCI ARM Ampere A1 = linux/arm64)
#
# 설계 의도:
#   - 무거운 Gradle 컴파일은 CI의 x86 러너에서 네이티브로 수행한다(빠름).
#   - JAR은 아키텍처 독립적이므로, 이 이미지는 산출물(app.jar)만 담는다.
#   - 따라서 buildx로 arm64 이미지를 만들어도 QEMU 에뮬레이션 컴파일이 없어 가볍다.
#
# 전제:
#   - docker build 이전에 `./gradlew bootJar` 가 실행되어
#     backend/build/libs/app.jar 가 존재해야 한다. (build.gradle에서 파일명 고정)
# =========================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

# 컨테이너 안에서 비루트 유저로 실행 (보안 기본).
RUN groupadd --system app && useradd --system --gid app app

# CI가 미리 빌드한 실행 가능 부트 JAR만 복사.
COPY build/libs/app.jar app.jar

USER app

EXPOSE 8080

# 프로파일/시크릿 등 모든 런타임 값은 docker-compose의 환경변수로 주입한다.
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
