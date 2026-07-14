package com.white.handdam.storage;

/**
 * 파일 업로드 성공 결과.
 * board_post_image 테이블의 storage_key / url / original_name 에 매핑된다.
 */
public record StoredObject(
	/** S3 객체 키 (삭제·재조회용). 예: premium-board/1/uuid_a.jpg */
	String storageKey,
	/** 클라이언트가 접근할 URL */
	String url,
	/** 업로드 당시 원본 파일명 */
	String originalName
) {
}
