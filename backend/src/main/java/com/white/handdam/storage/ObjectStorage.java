package com.white.handdam.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 업로드 포트.
 * board 등 도메인은 S3 구현을 직접 알지 않고 이 인터페이스만 사용한다.
 */
public interface ObjectStorage {

	/**
	 * 파일을 스토리지에 업로드한다.
	 *
	 * @param folder 저장 경로 prefix (예: premium-board/1)
	 * @param file   업로드할 파일
	 * @return 저장된 객체의 key, 접근 URL, 원본 파일명
	 */
	StoredObject upload(String folder, MultipartFile file);
}
