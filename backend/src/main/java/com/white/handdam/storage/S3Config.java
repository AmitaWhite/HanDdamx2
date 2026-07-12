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

	@Bean
	public S3Client s3Client(AwsProperties awsProperties) {
		var builder = S3Client.builder()
			.region(Region.of(awsProperties.getRegion()))
			.credentialsProvider(StaticCredentialsProvider.create(
				AwsBasicCredentials.create(
					awsProperties.getCredentials().getAccessKey(),
					awsProperties.getCredentials().getSecretKey()
				)
			));

		// LocalStack 등 커스텀 엔드포인트가 있으면 실제 AWS 대신 그 주소로 요청
		String endpoint = awsProperties.getS3().getEndpoint();
		if (endpoint != null && !endpoint.isBlank()) {
			builder
				.endpointOverride(URI.create(endpoint))
				// LocalStack은 path-style (endpoint/bucket/key) 이 필요
				.serviceConfiguration(S3Configuration.builder()
					.pathStyleAccessEnabled(true)
					.build());
		}

		return builder.build();
	}
}
