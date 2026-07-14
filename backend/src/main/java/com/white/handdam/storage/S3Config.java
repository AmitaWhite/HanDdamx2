package com.white.handdam.storage;

import java.net.URI;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

/**
 * AWS S3 클라이언트 빈 설정.
 * endpoint 가 있으면 LocalStack용 path-style 접근을 켠다.
 */
@Configuration
@EnableConfigurationProperties(AwsProperties.class)
public class S3Config {

	/**
	 * S3Client 빈 생성.
	 * AwsProperties 의 region / credentials / endpoint 로 클라이언트를 조립한다.
	 * 어느 리전으로, 어떤 계정(키)으로 S3 API를 호출할지를 클라이언트에 박아 두는 단계
	 */
	@Bean
	public S3Client s3Client(AwsProperties awsProperties) {
		// 기본: 리전 + 액세스 키로 S3 클라이언트 빌더 구성
		var builder = S3Client.builder()
			// 요청을 보낼 AWS 리전 (예: ap-northeast-2)
			.region(Region.of(awsProperties.getRegion()))
			// 인증: accessKey / secretKey 를 고정으로 사용
			.credentialsProvider(StaticCredentialsProvider.create(
				AwsBasicCredentials.create(
					awsProperties.getCredentials().getAccessKey(),
					awsProperties.getCredentials().getSecretKey()
				)
			));

		// 설정에 endpoint가 있으면 로컬 가짜 S3로, 없으면 진짜 AWS로 같은 S3Client 코드를 쓰게 하는 부분
		String endpoint = awsProperties.getS3().getEndpoint();
		if (endpoint != null && !endpoint.isBlank()) {
			builder
				// 예: http://localhost:4566 → LocalStack S3
				.endpointOverride(URI.create(endpoint))
				// LocalStack은 path-style (endpoint/bucket/key) 이 필요
				// virtual-hosted 방식(bucket.endpoint/...)은 LocalStack에서 자주 깨짐
				.serviceConfiguration(S3Configuration.builder()
					.pathStyleAccessEnabled(true)
					.build());
		}
		// endpoint 가 비어 있으면 AWS 기본 S3 엔드포인트 사용 (prod)

		// 설정이 끝난 클라이언트를 Spring 빈으로 등록
		return builder.build();
	}
}
