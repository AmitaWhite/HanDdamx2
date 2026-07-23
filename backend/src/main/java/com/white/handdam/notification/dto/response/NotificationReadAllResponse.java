package com.white.handdam.notification.dto.response;

/**
 * 알림 전체 읽음 처리 결과.
 *
 * <p>채팅의 일괄 읽음({@code ChatReadResponse})과 동일하게 갱신 건수를 돌려준다.
 */
public record NotificationReadAllResponse(
		long updatedCount) {
}
