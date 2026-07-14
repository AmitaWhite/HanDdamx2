package com.white.handdam.storage;

import com.white.handdam.global.exception.CommonErrorCode;
import com.white.handdam.global.exception.CustomException;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
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
		if (file == null || file.isEmpty()) {
			throw new CustomException(CommonErrorCode.INVALID_REQUEST, "이미지 파일이 비어 있습니다.");
		}

		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
			throw new CustomException(CommonErrorCode.INVALID_REQUEST, "지원하지 않는 이미지 형식입니다.");
		}

		String originalName = StringUtils.cleanPath(
			file.getOriginalFilename() == null ? "image" : file.getOriginalFilename()
		);
		String storageKey = folder + "/" + UUID.randomUUID() + "_" + originalName;
		String bucket = awsProperties.getS3().getBucket();

		try {
			s3Client.putObject(
				PutObjectRequest.builder()
					.bucket(bucket)
					.key(storageKey)
					.contentType(contentType)
					.contentLength(file.getSize())
					.build(),
				RequestBody.fromInputStream(file.getInputStream(), file.getSize())
			);
		} catch (IOException e) {
			throw new CustomException(CommonErrorCode.INTERNAL_ERROR, "이미지 업로드에 실패했습니다.");
		} catch (Exception e) {
			throw new CustomException(CommonErrorCode.INTERNAL_ERROR, "이미지 업로드에 실패했습니다.");
		}

		return new StoredObject(storageKey, buildUrl(bucket, storageKey), originalName);
	}

	/**
	 * 접근 URL 생성.
	 * LocalStack: http://localhost:4566/{bucket}/{key}
	 * AWS: https://{bucket}.s3.{region}.amazonaws.com/{key}
	 */
	private String buildUrl(String bucket, String storageKey) {
		String endpoint = awsProperties.getS3().getEndpoint();
		if (endpoint != null && !endpoint.isBlank()) {
			String normalized = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
			return normalized + "/" + bucket + "/" + storageKey;
		}
		return "https://" + bucket + ".s3." + awsProperties.getRegion() + ".amazonaws.com/" + storageKey;
	}
}
