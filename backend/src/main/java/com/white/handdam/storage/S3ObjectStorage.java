package com.white.handdam.storage;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * ObjectStorage 의 S3 구현체.
 * 이미지 검증 후 S3(또는 LocalStack)에 업로드하고 StoredObject 를 반환한다.
 */
@Component
@RequiredArgsConstructor
public class S3ObjectStorage implements ObjectStorage {

	/** 허용하는 이미지 MIME 타입 */
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
		"image/jpeg",
		"image/png",
		"image/gif",
		"image/webp"
	);

	private final S3Client s3Client;
	private final AwsProperties awsProperties;

	/**
	 * 파일을 S3(또는 LocalStack)에 업로드한다.
	 *
	 * <pre>
	 * 1) 파일/형식 검증
	 * 2) storageKey 생성 (folder/uuid_원본파일명)
	 * 3) S3 putObject
	 * 4) url 포함한 StoredObject 반환
	 * </pre>
	 *
	 * @param folder 저장 경로 prefix (예: premium-board/1)
	 * @param file   multipart 이미지 파일
	 */
	@Override
	public StoredObject upload(String folder, MultipartFile file) {
		// 1) 빈 파일 거부
		if (file == null || file.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일이 비어 있습니다.");
		}

		// jpeg/png/gif/webp 만 허용. 그 외 MIME → 400
		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 이미지 형식입니다.");
		}

		// 원본 파일명 정리 (.. 등 path traversal 문자 제거). 없으면 "image"
		//같은 경로 조작 문자를 정리해서 파일명이 상위 폴더로 새지 않게 함
		String originalName = StringUtils.cleanPath(
			file.getOriginalFilename() == null ? "image" : file.getOriginalFilename()
		);
		// 같은 파일명이어도 덮어쓰지 않도록 UUID 를 붙인다
		// 예: premium-board/1/a1b2c3_photo.jpg
		String storageKey = folder + "/" + UUID.randomUUID() + "_" + originalName;
		String bucket = awsProperties.getS3().getBucket();

		try {
			// 2) S3에 객체 저장 (메타: contentType, contentLength + 바이트 스트림)
			s3Client.putObject(
				PutObjectRequest.builder()
					.bucket(bucket)                 // 대상 버킷
					.key(storageKey)                // 객체 키 (= 경로)
					.contentType(contentType)
					.contentLength(file.getSize())
					.build(),
				RequestBody.fromInputStream(file.getInputStream(), file.getSize())
			);
		} catch (IOException e) {
			// 파일 스트림 읽기 실패
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다.", e);
		} catch (Exception e) {
			// S3/LocalStack 통신·권한 등 기타 실패
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드에 실패했습니다.", e);
		}

		// 3) DB에 넣을 storageKey / 접근 URL / 원본 파일명 반환
		return new StoredObject(storageKey, buildUrl(bucket, storageKey), originalName);
	}

	/**
	 * 접근 URL 생성.
	 * LocalStack: http://localhost:4566/{bucket}/{key}
	 * AWS: https://{bucket}.s3.{region}.amazonaws.com/{key}
	 */
	private String buildUrl(String bucket, String storageKey) {
		String endpoint = awsProperties.getS3().getEndpoint();
		// LocalStack 등 커스텀 엔드포인트 → path-style URL
		if (endpoint != null && !endpoint.isBlank()) {
			// 끝 슬래시 중복 방지
			String normalized = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
			return normalized + "/" + bucket + "/" + storageKey;
		}
		// 실제 AWS S3 virtual-hosted URL
		return "https://" + bucket + ".s3." + awsProperties.getRegion() + ".amazonaws.com/" + storageKey;
	}
}
