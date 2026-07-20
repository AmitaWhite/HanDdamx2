package com.white.handdam.creator.event;

/**
 * 크리에이터 전환 신청이 거절되었을 때 발행하는 도메인 이벤트.
 * 네이밍 규칙: {Aggregate}{PastTenseVerb}Event
 *
 * <p>{@code notification} 패키지의 {@code CreatorNotificationListener} 가 구독해
 * 신청자에게 {@code CREATOR_APPLICATION_REJECTED} 알림을 만든다.
 * 거절 사유가 알림 문구에 그대로 들어간다.
 *
 * <h3>발행 위치 — save() 호출이 없다</h3>
 * {@code creator/service/CreatorApplicationService#reject} — {@code @Transactional} 안,
 * {@code application.reject(...)} 직후 {@code return} 직전 (현재 173번 줄 부근).
 *
 * <p>{@link CreatorApplicationApprovedEvent} 와 마찬가지로 더티 체킹 경로다.
 * {@code approve} 와 달리 {@code Member} 엔티티는 로드되지 않지만,
 * 필요한 건 {@code application.getMemberId()} 뿐이라 문제없다.
 *
 * <h3>발행 예시</h3>
 * <pre>{@code
 * application.reject(adminId, request.rejectReason());
 * eventPublisher.publishEvent(new CreatorApplicationRejectedEvent(
 *     applicationId,                // 파라미터
 *     application.getMemberId(),    // 신청자 = 알림 수신자
 *     request.rejectReason()
 * ));
 * }</pre>
 *
 * <h3>시스템 알림 — senderId 는 null</h3>
 * 거절한 관리자 ID 는 신청자에게 노출하지 않는다. 리스너가 {@code senderId = null} 로 만든다.
 *
 * @param applicationId 신청 ID (알림 클릭 시 이동 대상)
 * @param applicantId   신청자 = 알림 수신자 ({@code application.getMemberId()})
 * @param rejectReason  거절 사유 (알림 문구에 사용)
 */
public record CreatorApplicationRejectedEvent(
	Long applicationId,
	Long applicantId,
	String rejectReason
) {
}
