package com.white.handdam.storage;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 의 cloud.aws 설정을 담는 클래스.
 * 예: region, s3.endpoint(LocalStack), s3.bucket, credentials
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "cloud.aws")
public class AwsProperties {

	/** AWS 리전. 예: ap-northeast-2 */
	private String region;
	private S3 s3 = new S3();
	private Credentials credentials = new Credentials();

	@Getter
	@Setter
	public static class S3 {
		/**
		 * S3 엔드포인트.
		 * local: LocalStack 주소 (http://localhost:4566)
		 * prod: 비우면 실제 AWS S3 사용
		 */
		private String endpoint;
		/** 버킷 이름 */
		private String bucket;
		/**
		 * 브라우저 등 외부에서 접근할 공개 URL 베이스.
		 * 비어 있으면 endpoint를 그대로 쓴다(로컬 개발처럼 endpoint 자체가 이미 외부에서도 접근 가능한 경우).
		 * 배포 환경처럼 endpoint(컨테이너 내부 주소)와 실제 공개 주소가 다를 때만 채운다.
		 */
		private String publicUrl;
	}

	@Getter
	@Setter
	public static class Credentials {
		private String accessKey;
		private String secretKey;
	}
}
